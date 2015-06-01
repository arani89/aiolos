/*
 * Copyright (c) 2014, Tim Verbelen
 * Internet Based Communication Networks and Services research group (IBCN),
 * Department of Information Technology (INTEC), Ghent University - iMinds.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of Ghent University - iMinds, nor the names of its 
 *      contributors may be used to endorse or promote products derived from 
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package be.iminds.aiolos.event.broker.mqtt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import be.iminds.aiolos.event.broker.AbstractEventBroker;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

public class MQTTEventBroker extends AbstractEventBroker implements MqttCallback {

	private MqttClient mqtt;
	private Kryo kryo;
	
	public MQTTEventBroker(BundleContext context) throws Exception {
		super(context);
		
		String server = context.getProperty("aiolos.event.mqtt.server");
		if(server==null){
			throw new Exception("No MQTT server specified");
		}
		try {
			mqtt = new MqttClient(server, frameworkId);
		} catch(MqttException e){
			e.printStackTrace();
			throw new Exception("Failed to connect to MQTT Server "+server, e);
		}
		
		kryo = new Kryo();
		// we call reset ourselves after each readObject
		kryo.setAutoReset(false);
		// redirect to this bundle classloader
		kryo.setClassLoader(MQTTEventBroker.class.getClassLoader());
		// Sometimes problems with serializing exceptions in Kryo (e.g. Throwable discrepance between android/jdk)
		kryo.addDefaultSerializer(Throwable.class, JavaSerializer.class);
		// required to instantiate classes without no-arg constructor
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}

	public void start(){
		super.start();
		
		try {
			mqtt.connect();
			mqtt.setCallback(this);
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void stop(){
		super.stop();
		
		try {
			mqtt.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void addTopic(String topic){
		super.addTopic(topic);
		try {
			mqtt.subscribe("aiolos/"+topic.replaceAll("\\*", "#"));
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void removeTopic(String topic){
		super.removeTopic(topic);
		try {
			mqtt.unsubscribe("aiolos/"+topic.replaceAll("\\*", "#"));
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void forwardEvent(Event event) {
		Output out = null;
		try {
			Map<String, Object> properties = new HashMap<String, Object>();
			
			for(String key : event.getPropertyNames()){
				properties.put(key, event.getProperty(key));
			}
			properties.put(Constants.FRAMEWORK_UUID, frameworkId);
		
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			out = new Output(baos);
			kryo.writeClassAndObject(out, properties);
			out.flush();
			
			MqttMessage message = new MqttMessage(baos.toByteArray());
			mqtt.publish("aiolos/"+event.getTopic(), message);
			
		} catch(Exception e){
			e.printStackTrace();
		} finally {
			kryo.reset();
			out.close();
		}
	}

	@Override
	public void connectionLost(Throwable ex) {
		// TODO handle this
		ex.printStackTrace();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {	
		// TODO handle this
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		EventAdmin ea = eventAdminTracker.getService();
		if(ea!=null){
			//  remove first aiolos/
			String osgiTopic = topic.substring(7);
			
			// parse osgi event
			Input in = null;
			try {
				in = new Input(new ByteArrayInputStream(message.getPayload()));
				Map<String, Object> properties = (Map<String, Object>) kryo.readClassAndObject(in);
	
				if(!properties.get(Constants.FRAMEWORK_UUID).equals(frameworkId)){
					Event event = new Event(osgiTopic, properties);
					ea.postEvent(event);
				}
			
			} catch(Exception e){
				e.printStackTrace();
			} finally{
				kryo.reset();
				in.close();
			}
		}
	}
}
