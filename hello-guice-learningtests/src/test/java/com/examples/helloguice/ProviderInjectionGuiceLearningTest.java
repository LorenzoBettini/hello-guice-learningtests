package com.examples.helloguice;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ProviderInjectionGuiceLearningTest {

	static class MyClass {
		@Inject
		Provider<MyInterface> myInterfaceProvider;

		public MyInterface getField() {
			return myInterfaceProvider.get();
		}

	};

	static interface MyInterface {

	}

	static class MyImplementation implements MyInterface {

	};

	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(MyInterface.class).to(MyImplementation.class);
		}

	}

	@Test
	public void test() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClass a = injector.getInstance(MyClass.class);
		assertEquals(MyImplementation.class, a.getField().getClass());
	}

}
