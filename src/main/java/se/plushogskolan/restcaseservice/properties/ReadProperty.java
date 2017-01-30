package se.plushogskolan.restcaseservice.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReadProperty {

	public static String readProperty(String propertyName) throws IOException {

		Properties prop = new Properties();
		InputStream input = null;

		input = new FileInputStream("src/main/resources/application.properties");

		prop.load(input);

		String property = prop.getProperty(propertyName);

		input.close();
		
		return property;
	}
}
