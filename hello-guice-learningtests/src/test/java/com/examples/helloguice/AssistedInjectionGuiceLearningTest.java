package com.examples.helloguice;

import static org.junit.Assert.*;

import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class AssistedInjectionGuiceLearningTest {

	static class MyClass {
		private MyInterface field;
		private String aString;
		private int anInt;

		@Inject
		public MyClass(MyInterface field, @Assisted String aString, @Assisted int anInt) {
			this.field = field;
			this.aString = aString;
			this.anInt = anInt;
		}

		public MyInterface getField() {
			return field;
		}

		public String getaString() {
			return aString;
		}

		public int getAnInt() {
			return anInt;
		}
	};

	static interface MyClassFactory {
		public MyClass create(String aString, int anInt);
	}

	static interface MyInterface {

	}

	static class MyImplementation implements MyInterface {

	};

	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(MyInterface.class).to(MyImplementation.class);
			install(new FactoryModuleBuilder().build(MyClassFactory.class));
		}

	}

	@Test
	public void test() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClassFactory factory = injector.getInstance(MyClassFactory.class);
		MyClass a = factory.create("a String", 1);
		assertEquals(MyImplementation.class, a.getField().getClass());
		assertEquals("a String", a.getaString());
		assertEquals(1, a.getAnInt());
	}

}
