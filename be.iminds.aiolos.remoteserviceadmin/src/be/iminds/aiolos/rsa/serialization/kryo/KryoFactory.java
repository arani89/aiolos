package be.iminds.aiolos.rsa.serialization.kryo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

/**
 * This is a dirty workaround to have a Kryo instance to which we can easily
 * add/remove Kryo serializers from services ... could be solved more elegantly
 * by actually using the OSGi service mechanism for Serializers/Deserializers and 
 * NetworkChannelFactory etc.
 */
public class KryoFactory {

	private static Map<Class, Serializer> serializers = new HashMap<Class, Serializer>(); 
	
	public static Kryo createKryo(){
		Kryo kryo = new Kryo();
		
		// we call reset ourselves after each readObject
		kryo.setAutoReset(false);
		// redirect to RSA bundle classloader
		kryo.setClassLoader(KryoFactory.class.getClassLoader());
		
		// Sometimes problems with serializing exceptions in Kryo (e.g. Throwable discrepance between android/jdk)
		kryo.addDefaultSerializer(Throwable.class, JavaSerializer.class);
		// required to instantiate classes without no-arg constructor
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		// required to correctly handle unmodifiable collections (i.e. used in EndpointDescription)
		UnmodifiableCollectionsSerializer.registerSerializers( kryo );
		// required to correctly handle Arrays$ArrayList class (i.e. used in EndpointDescription)
		kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );		
		
		for(Iterator<Entry<Class, Serializer>> it = serializers.entrySet().iterator();it.hasNext();){
			Entry<Class, Serializer> entry = it.next();
			kryo.addDefaultSerializer(entry.getKey(), entry.getValue());
		}
		
		return kryo;
	}
	
	public static void addSerializer(String clazz, Object serializer){
		try {
			// This is fucked up dirty... but was quickest way to get stuff working...
			serializers.put(KryoFactory.class.getClassLoader().loadClass(clazz), (Serializer)serializer);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void removeSerializer(String clazz, Object serializer){
		try {
			// This is fucked up dirty... but was quickest way to get stuff working...
			serializers.remove(KryoFactory.class.getClassLoader().loadClass(clazz));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
