package com.examples.helloguice;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ChildInjectionGuiceLearningTest {

	static interface MyParam {

	}

	static class MyParamImpl implements MyParam {

	}

	static class MyClass {
		private MyInterface field;
		private MyParam myParam;

		@Inject
		public MyClass(MyInterface field, MyParam myParam) {
			this.field = field;
			this.myParam = myParam;
		}

		public MyInterface getField() {
			return field;
		}

		public MyParam getMyParam() {
			return myParam;
		}

	};

	static interface MyInterface {

	}

	static class MyImplementation implements MyInterface {

	};

	/**
	 * {@link MyParam} is not bound.
	 */
	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(MyInterface.class).to(MyImplementation.class);
		}

	}

	/**
	 * Fails since {@link MyParam} is not bound
	 */
	@Test(expected = ConfigurationException.class)
	public void testMissingBinding() {
		Injector injector = Guice.createInjector(new MyModule());
		injector.getInstance(MyClass.class);
	}

	/**
	 * Binds {@link MyParam} in the child injector
	 */
	@Test
	public void testChildInjectorBinding() {
		Injector injector = Guice.createInjector(new MyModule()).createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(MyParam.class).to(MyParamImpl.class);
			}
		});
		MyClass a = injector.getInstance(MyClass.class);
		assertEquals(MyImplementation.class, a.getField().getClass());
		assertEquals(MyParamImpl.class, a.getMyParam().getClass());
	}

	/**
	 * Fails because the binding of the child injector is not available in the
	 * parent injector.
	 */
	@Test(expected = ConfigurationException.class)
	public void testChildInjectorBindingNotAvailableInTheParent() {
		Injector parentInjector = Guice.createInjector(new MyModule());
		parentInjector.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(MyParam.class).to(MyParamImpl.class);
			}
		});
		parentInjector.getInstance(MyClass.class);
	}

}
