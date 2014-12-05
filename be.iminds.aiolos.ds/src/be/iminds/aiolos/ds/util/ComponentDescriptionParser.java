package be.iminds.aiolos.ds.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import be.iminds.aiolos.ds.component.ComponentDescription;
import be.iminds.aiolos.ds.component.ComponentDescription.ConfigurationPolicy;
import be.iminds.aiolos.ds.component.ReferenceDescription;
import be.iminds.aiolos.ds.component.ReferenceDescription.Policy;
import be.iminds.aiolos.ds.component.ReferenceDescription.PolicyOption;
import be.iminds.aiolos.ds.component.ServiceDescription;
import be.iminds.aiolos.ds.component.ReferenceDescription.Cardinality;

public class ComponentDescriptionParser {

	public static ComponentDescription parse(InputStream stream) throws Exception {
		ComponentDescription component = null;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		KXmlParser parser = new KXmlParser();
		parser.setInput(in);

		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "scr:component");
		
		while (parser.getEventType() != XmlPullParser.END_DOCUMENT) { // parser.nextTag()
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				
				if(parser.getName().equals("scr:component")){
					component = parseComponent(parser);
				} else if(parser.getName().equals("implementation")){
					String clazz = parseImplementationClass(parser);
					component.setImplementationClass(clazz);
				} else if(parser.getName().equals("service")){
					ServiceDescription service = parseServiceDescription(parser);
					component.addService(service);
				} else if(parser.getName().equals("reference")){
					ReferenceDescription reference = parseReferenceDescription(parser);
					component.addReference(reference);
				} else if(parser.getName().equals("property")){
					String key = parser.getAttributeValue(null, "name");
					Object value = parseProperty(parser);
					component.setProperty(key, value);
				}
			} 
			parser.next();
		}
		
		return component;
	}
	
	private static ComponentDescription parseComponent(KXmlParser parser) throws Exception{
		ComponentDescription component = new ComponentDescription();
		for(int i=0;i<parser.getAttributeCount();i++){
			String attribute = parser.getAttributeName(i);
			if(attribute.equals("name")){
				component.setName(parser.getAttributeValue(i));
			} else if(attribute.equals("enabled")){
				component.setEnabled(Boolean.parseBoolean(parser.getAttributeValue(i)));
			} else if(attribute.equals("factory")){
				component.setFactory(!parser.getAttributeValue(i).isEmpty());
			} else if(attribute.equals("immediate")){
				component.setImmediate(Boolean.parseBoolean(parser.getAttributeValue(i)));
			} else if(attribute.equals("configuration-policy")){
				component.setConfigurationPolicy(ConfigurationPolicy.toConfigurationPolicy(parser.getAttributeValue(i)));
			} else if(attribute.equals("configuration-pid")){
				component.setConfigurationPID(parser.getAttributeValue(i));
			} else if(attribute.equals("activate")){
				component.setActivate(parser.getAttributeValue(i));
			} else if(attribute.equals("deactivate")){
				component.setDeactivate(parser.getAttributeValue(i));
			} else if(attribute.equals("modified")){
				component.setModified(parser.getAttributeValue(i));
			}
		}
		return component;
	}
	
	private static String parseImplementationClass(KXmlParser parser) throws Exception{
		String clazz = null;
		for(int i=0;i<parser.getAttributeCount();i++){
			String attribute = parser.getAttributeName(i);
			if(attribute.equals("class")){
				clazz = parser.getAttributeValue(i);
			}
		}
		return clazz;
	}
	
	private static ServiceDescription parseServiceDescription(KXmlParser parser) throws Exception{
		List<String> ifaces = new ArrayList<String>();
		boolean factory = false;
		
		while(!(parser.getEventType()==XmlPullParser.END_TAG
				&& parser.getName().equals("service"))){
			if(parser.getName()==null){
				// ignore
			} else if(parser.getName().equals("service")){
				for(int i=0;i<parser.getAttributeCount();i++){
					String attribute = parser.getAttributeName(i);
					if(attribute.equals("servicefactory")){
						factory = Boolean.parseBoolean(parser.getAttributeValue(i));
					}
				}
			} else if(parser.getName().equals("provide")){
				for(int i=0;i<parser.getAttributeCount();i++){
					String attribute = parser.getAttributeName(i);
					if(attribute.equals("interface")){
						String iface = parser.getAttributeValue(i);
						ifaces.add(iface);
					}
				}
			}
			parser.next();
		}
		
		ServiceDescription service = new ServiceDescription(ifaces.toArray(new String[ifaces.size()]));
		service.setServiceFactory(factory);
		return service;
	}
	
	private static ReferenceDescription parseReferenceDescription(KXmlParser parser){
		ReferenceDescription reference = new ReferenceDescription();
		for(int i=0;i<parser.getAttributeCount();i++){
			String attribute = parser.getAttributeName(i);
			if(attribute.equals("name")){
				reference.setName(parser.getAttributeValue(i));
			} else if(attribute.equals("interface")){
				reference.setInterface(parser.getAttributeValue(i));
			} else if(attribute.equals("cardinality")){
				reference.setCardinality(Cardinality.toCardinality(parser.getAttributeValue(i)));
			} else if(attribute.equals("policy")){
				reference.setPolicy(Policy.toPolicy(parser.getAttributeValue(i)));
			} else if(attribute.equals("policy-option")){
				reference.setPolicyOption(PolicyOption.toPolicyOption(parser.getAttributeValue(i)));
			} else if(attribute.equals("target")){
				reference.setTarget(parser.getAttributeValue(i));
			} else if(attribute.equals("bind")){
				reference.setBind(parser.getAttributeValue(i));
			} else if(attribute.equals("updated")){
				reference.setUpdated(parser.getAttributeValue(i));
			}else if(attribute.equals("unbind")){
				reference.setUnbind(parser.getAttributeValue(i));
			}
		}
		return reference;
	}
	
	private static Object parseProperty(KXmlParser parser){
		String valueType = parser.getAttributeValue(null, "type");
		String value = parser.getAttributeValue(null, "value");
		
		if(value==null){
			// parse body
			String body = parser.getText();
			StringTokenizer st = new StringTokenizer(body, "\n");
			List<String> tokens = new ArrayList<String>();
			while(st.hasMoreTokens()){
				String token = st.nextToken();
				if(token!=null && !token.isEmpty()){
					tokens.add(token);
				}
			}
			
			if(valueType==null || valueType.equals("String")){
				String[] array = new String[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = tokens.get(i);
				}
				return array;
			} else if(valueType.equals("long") || valueType.equals("Long")){
				long[] array = new long[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = Long.parseLong(tokens.get(i));
				}
				return array;
			} else if(valueType.equals("double") || valueType.equals("Double")){
				double[] array = new double[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = Double.parseDouble(tokens.get(i));
				}
				return array;
			} else if(valueType.equals("float") || valueType.equals("Float")){
				float[] array = new float[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = Float.parseFloat(tokens.get(i));
				}
				return array;
			} else if(valueType.equals("int") || valueType.equals("Integer")){
				int[] array = new int[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = Integer.parseInt(tokens.get(i));
				}
				return array;
			} else if(valueType.equals("byte") || valueType.equals("Byte")){
				byte[] array = new byte[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = Byte.parseByte(tokens.get(i));
				}
				return array;
			} else if(valueType.equals("char") || valueType.equals("Character")){
				char[] array = new char[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = value.charAt(0);
				}
				return array;
			} else if(valueType.equals("boolean") || valueType.equals("Boolean")){
				boolean[] array = new boolean[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = Boolean.parseBoolean(tokens.get(i));
				}
				return array;
			} else if(valueType.equals("short") || valueType.equals("Short")){
				short[] array = new short[tokens.size()];
				for(int i=0;i<tokens.size();i++){
					array[i] = Short.parseShort(tokens.get(i));
				}
				return array;			
			} 
		} else {
			// convert value
			if(valueType==null || valueType.equals("String")){
				return value;
			} else if(valueType.equals("long") || valueType.equals("Long")){
				return Long.parseLong(value);
			} else if(valueType.equals("double") || valueType.equals("Double")){
				return Double.parseDouble(value);
			} else if(valueType.equals("float") || valueType.equals("Float")){
				return Float.parseFloat(value);
			} else if(valueType.equals("int") || valueType.equals("Integer")){
				return Integer.parseInt(value);
			} else if(valueType.equals("byte") || valueType.equals("Byte")){
				return Byte.parseByte(value);
			} else if(valueType.equals("char") || valueType.equals("Character")){
				return value.charAt(0);
			} else if(valueType.equals("boolean") || valueType.equals("Boolean")){
				return Boolean.parseBoolean(value);
			} else if(valueType.equals("short") || valueType.equals("Short")){
				return Short.parseShort(value);
			} 
		}
		
		return value;
	}
}
