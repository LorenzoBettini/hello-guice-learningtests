package com.examples.helloguice;

import static org.junit.Assert.*;

import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class MethodInjectionGuiceLearningTest {

	static class MyClass {
		private MyInterface field;
		private MyInterface later;

		@Inject
		public MyClass(MyInterface field) {
			this.field = field;
		}

		@Inject
		private void calledAfterConstructor(MyInterface later) {
			this.field.getClass(); // just to make sure it's not null
			this.later = later;
		}

		@Inject
		public void calledAfterConstructor2() {
			this.field.getClass(); // just to make sure it's not null
		}

		public MyInterface getField() {
			return field;
		}

		public MyInterface getLater() {
			return later;
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
		assertEquals(MyImplementation.class, a.getLater().getClass());
	}

}
