package com.examples.helloguice;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;

public class ChildInjectionAndMultibindingsGuiceLearningTest {

	private static class MyObject {

	}

	private static class WithSetOfMyObject {
		public Set<MyObject> myObjects;

		@Inject
		WithSetOfMyObject(Set<MyObject> myObjects) {
			this.myObjects = myObjects;
		}
	}

	/**
	 * No implementation for Set&lt;MyObject&gt; was bound
	 */
	@Test(expected = ConfigurationException.class)
	public void testNoMultibindings() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {

			}
		});
		injector.getInstance(WithSetOfMyObject.class);
	}

	@Test
	public void testEmptyMultibindings() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				Multibinder.newSetBinder(binder(), MyObject.class);
			}
		});
		assertTrue(injector.getInstance(WithSetOfMyObject.class).myObjects.isEmpty());
	}

	@Test
	public void testNotEmptyMultibindings() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				Multibinder
					.newSetBinder(binder(), MyObject.class)
					.addBinding()
					.toInstance(new MyObject());
			}
		});
		assertEquals(1, injector.getInstance(WithSetOfMyObject.class).myObjects.size());
	}

	/**
	 * Set&lt;MyObject&gt; was already bound in the parent injector
	 */
	@Test(expected = CreationException.class)
	public void testMultibindingsWithChildInjector() {
		Injector parent = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				Multibinder.newSetBinder(binder(), MyObject.class);
			}
		});
		Injector injector = parent.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				Multibinder
					.newSetBinder(binder(), MyObject.class)
					.addBinding()
					.toInstance(new MyObject());
			}
		});
		assertTrue(injector.getInstance(WithSetOfMyObject.class).myObjects.isEmpty());
	}
}
