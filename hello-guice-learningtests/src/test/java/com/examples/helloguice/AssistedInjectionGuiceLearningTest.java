package com.examples.helloguice;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class AssistedInjectionGuiceLearningTest {

	static interface MyClassInterface {

		MyInterface getField();

		String getaString();

		int getAnInt();

	}

	static class MyClass implements MyClassInterface {
		private MyInterface field;
		private String aString;
		private int anInt;

		@Inject
		public MyClass(MyInterface field, @Assisted String aString, @Assisted int anInt) {
			this.field = field;
			this.aString = aString;
			this.anInt = anInt;
		}

		@Override
		public MyInterface getField() {
			return field;
		}

		@Override
		public String getaString() {
			return aString;
		}

		@Override
		public int getAnInt() {
			return anInt;
		}
	};

	static interface MyClassFactory {
		public MyClassInterface create(String aString, int anInt);
	}

	static interface MyInterface {

	}

	static class MyImplementation implements MyInterface {

	};

	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(MyInterface.class).to(MyImplementation.class);
			install(new FactoryModuleBuilder()
				.implement(MyClassInterface.class, MyClass.class)
				.build(MyClassFactory.class));
		}

	}

	@Test
	public void test() {
		Injector injector = Guice.createInjector(new MyModule());
		MyClassFactory factory = injector.getInstance(MyClassFactory.class);
		MyClassInterface a = factory.create("a String", 1);
		assertEquals(MyImplementation.class, a.getField().getClass());
		assertEquals("a String", a.getaString());
		assertEquals(1, a.getAnInt());
	}

	/**
	 * When binding MyClass it can find bindings for String and Integer
	 * annotated.
	 */
	@Test(expected=CreationException.class)
	public void testWithBindingForTheImplementationBeforeFactory() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(MyInterface.class).to(MyImplementation.class);
				bind(MyClassInterface.class).to(MyClass.class);
				install(new FactoryModuleBuilder()
					.build(MyClassFactory.class));
			}
		});
		injector.getInstance(MyClassFactory.class);
	}

	/**
	 * When binding MyClass it can find bindings for String and Integer
	 * annotated.
	 */
	@Test(expected=CreationException.class)
	public void testWithBindingForTheImplementationAfterFactory() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(MyInterface.class).to(MyImplementation.class);
				install(new FactoryModuleBuilder()
					.build(MyClassFactory.class));
				bind(MyClassInterface.class).to(MyClass.class);
			}
		});
		injector.getInstance(MyClassFactory.class);
	}

	/**
	 * When binding MyClass it can find bindings for String and Integer
	 * annotated.
	 */
	@Test(expected=CreationException.class)
	public void testWithBindingForTheImplementationAndInFactory() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(MyInterface.class).to(MyImplementation.class);
				install(new FactoryModuleBuilder()
					.implement(MyClassInterface.class, MyClass.class)
					.build(MyClassFactory.class));
				bind(MyClassInterface.class).to(MyClass.class);
			}
		});
		injector.getInstance(MyClassFactory.class);
	}

}
