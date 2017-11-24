package com.examples.helloguice;

import static org.junit.Assert.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class SingletonInjectionGuiceLearningTest {

	static class MyClass1 {
		private MyInterface field;

		@Inject
		public MyClass1(MyInterface field) {
			this.field = field;
		}

		public MyInterface getField() {
			return field;
		}
	};

	static class MyClass2 {
		@Inject
		private MyInterface field;

		public MyInterface getField() {
			return field;
		}
	};

	static interface MyInterface {

	}

	@Singleton
	static class MyImplementation implements MyInterface {

	};

	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(MyInterface.class).to(MyImplementation.class);
		}

	}

	@Test
	public void testSingleton() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClass1 a1 = injector.getInstance(MyClass1.class);
		MyClass2 a2 = injector.getInstance(MyClass2.class);
		assertSame(a1.getField(), a2.getField());
	}

	@Test
	public void testSingletonWithDifferentInjectors() {
		MyClass1 a1 = Guice.createInjector(new MyModule()).getInstance(MyClass1.class);
		MyClass2 a2 = Guice.createInjector(new MyModule()).getInstance(MyClass2.class);
		assertNotSame(a1.getField(), a2.getField());
	}

}
