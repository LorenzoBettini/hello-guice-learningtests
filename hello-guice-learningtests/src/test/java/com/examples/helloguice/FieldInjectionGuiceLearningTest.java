package com.examples.helloguice;

import static org.junit.Assert.*;

import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class FieldInjectionGuiceLearningTest {

	static class MyClass1 {
		@Inject
		private MyInterface field;

		public MyInterface getField() {
			return field;
		}
	};

	static class MyClass2 {
		@Inject
		private MyInterface field;

		public MyClass2() {
			// required by Guice in the presence of
			// a non annotated constructor with parameters
		}

		public MyClass2(MyInterface field) {
			this.field = field;
		}

		public MyInterface getField() {
			return field;
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
	public void testClass1() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClass1 a = injector.getInstance(MyClass1.class);
		assertEquals(MyImplementation.class, a.getField().getClass());
	}

	@Test
	public void testClass2() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClass2 a = injector.getInstance(MyClass2.class);
		assertEquals(MyImplementation.class, a.getField().getClass());
	}

}
