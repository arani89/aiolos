package be.iminds.aiolos.event.serializer;

import java.util.HashMap;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventProperties;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class EventSerializer extends Serializer<Event> {

	@Override
	public Event read(Kryo kryo, Input input, Class<Event> clazz) {
		String topic = input.readString();
		HashMap<String, Object> properties = (HashMap<String, Object>) kryo.readClassAndObject(input);
		Event e = new Event(topic, new EventProperties(properties));
		return e;
	}

	@Override
	public void write(Kryo kryo, Output output, Event event) {
		String topic = event.getTopic();
		HashMap<String, Object> properties = new HashMap<String, Object>();
		for(String k : event.getPropertyNames()){
			properties.put(k, event.getProperty(k));
		}
		output.writeString(topic);
		kryo.writeClassAndObject(output, properties);
	}

}
