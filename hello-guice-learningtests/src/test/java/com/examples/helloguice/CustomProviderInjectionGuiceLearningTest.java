package com.examples.helloguice;

import static org.junit.Assert.assertSame;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class CustomProviderInjectionGuiceLearningTest {

	static class MyClass1 {
		@Inject
		Provider<MyInterface> myInterfaceProvider;

		public MyInterface getField() {
			return myInterfaceProvider.get();
		}

	};

	static class MyClass2 {
		@Inject
		MyInterface field;

		public MyInterface getField() {
			return field;
		}

	};

	static interface MyInterface {

	}

	static class MyImplementation implements MyInterface {

	};

	// the fixed instance always returned by the custom provider
	static private MyInterface fixed = new MyImplementation();

	static class MyCustomProvider implements Provider<MyInterface> {

		@Override
		public MyInterface get() {
			return fixed;
		}

	}

	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(MyInterface.class).toProvider(MyCustomProvider.class);
		}

	}

	@Test
	public void testClassWithInjectedProvider() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClass1 a1 = injector.getInstance(MyClass1.class);
		MyClass1 a2 = injector.getInstance(MyClass1.class);
		assertSame(a1.getField(), a2.getField());
		assertSame(a1.getField(), fixed);
	}

	@Test
	public void testClassWithInjectedFieldImplicitlyCallsTheCustomProvider() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClass2 a3 = injector.getInstance(MyClass2.class);
		assertSame(a3.getField(), fixed);
	}

}
