package com.examples.helloguice;

import static org.junit.Assert.*;

import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ConstructorInjectionGuiceLearningTest {

	static class MyClass {
		private MyInterface field;

		@Inject
		public MyClass(MyInterface field) {
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
	public void test() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClass a = injector.getInstance(MyClass.class);
		assertEquals(MyImplementation.class, a.getField().getClass());
	}

}
