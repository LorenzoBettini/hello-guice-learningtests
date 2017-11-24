package com.examples.helloguice;

import static org.junit.Assert.*;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class NamedBindingInjectionGuiceLearningTest {

	static class MyClass1 {
		@Inject
		@Named("URL")
		private String url;

		@Inject
		@Named("port")
		private int port;

		@Override
		public String toString() {
			return "MyClass1 [url=" + url + ", port=" + port + "]";
		}
	};

	static class MyClass2 {
		@Inject
		@Named("URL")
		private String url;

		@Inject
		@Named("port")
		private int port;

		@Override
		public String toString() {
			return "MyClass2 [url=" + url + ", port=" + port + "]";
		}
	};

	static class MyLocalHostModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(String.class).
				annotatedWith(Names.named("URL")).
				toInstance("localhost");
			bind(int.class).
				annotatedWith(Names.named("port")).
				toInstance(8080);
		}

	}

	static class MyGoogleModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(String.class).
				annotatedWith(Names.named("URL")).
				toInstance("www.google.com");
			bind(int.class).
				annotatedWith(Names.named("port")).
				toInstance(80);
		}

	}

	@Test
	public void testWithLocalHost() {
		Injector injector = Guice.createInjector(new MyLocalHostModule());
		assertEquals("MyClass1 [url=localhost, port=8080]",
			injector.getInstance(MyClass1.class).toString());
		assertEquals("MyClass2 [url=localhost, port=8080]",
			injector.getInstance(MyClass2.class).toString());
	}

	@Test
	public void testWithGoogle() {
		Injector injector = Guice.createInjector(new MyGoogleModule());
		assertEquals("MyClass1 [url=www.google.com, port=80]",
			injector.getInstance(MyClass1.class).toString());
		assertEquals("MyClass2 [url=www.google.com, port=80]",
			injector.getInstance(MyClass2.class).toString());
	}

}
