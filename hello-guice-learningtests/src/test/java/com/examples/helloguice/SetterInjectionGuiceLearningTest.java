package com.examples.helloguice;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class SetterInjectionGuiceLearningTest {

	static class MyClass1 {
		private MyInterface field;

		public MyInterface getField() {
			return field;
		}

		@Inject
		public void setField(MyInterface field) {
			this.field = field;
		}
	};

	static class MyClass2 {
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

		@Inject
		public void setField(MyInterface field) {
			this.field = field;
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
