 /** 
 * Copyright (c) 2007-2008, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
*/
package org.cleartk.classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.CleartkException;
import org.cleartk.Initializable;
import org.cleartk.util.EmptyAnnotator;
import org.junit.Assert;
import org.junit.Test;
import org.uutuc.factory.AnalysisEngineFactory;
import org.uutuc.factory.TypeSystemDescriptionFactory;

/**
 * <br>Copyright (c) 2007-2008, Regents of the University of Colorado 
 * <br>All rights reserved.

 * 
 * @author Steven Bethard
 */
public class SequentialInstanceConsumer_ImplBaseTest {
	
	// class that sets the class-level producer variables
	public static class Handler implements SequentialAnnotationHandler<Object>, Initializable {
		public Handler() {
			SequentialInstanceConsumer_ImplBaseTest.producer = this;
		}
		public void initialize(UimaContext context) throws ResourceInitializationException {
			SequentialInstanceConsumer_ImplBaseTest.producerIsInitialized = true;
		}
		public void process(JCas cas, SequentialInstanceConsumer<Object> consumer) throws CleartkException {
				consumer.consumeSequence(SequentialInstanceConsumer_ImplBaseTest.instances);
		}
	}


	// class that tracks calls to methods and stores observed instances
	public class Consumer extends SequentialInstanceConsumer_ImplBase<Object> {
	
		public int consumeAllCount = 0;
		public List<Instance<Object>> instances;
		
		public Consumer() {
			this.instances = new ArrayList<Instance<Object>>();
		}

		public List<Object> consumeSequence(List<Instance<Object>> instances) {
			this.consumeAllCount++;
			this.instances.addAll(instances);
			return null;
		}
		public boolean expectsOutcomes() {
			return false;
		}

	}

	@Test
	public void testBadHandlerName() {
		try {
			AnalysisEngineFactory.createPrimitive(
					SequentialInstanceConsumer_ImplBaseTest.Consumer.class,
					TypeSystemDescriptionFactory.createTypeSystemDescription("org.cleartk.TypeSystem"),
					InstanceConsumer.PARAM_ANNOTATION_HANDLER, "Foo");
			Assert.fail("expected exception with bad AnnotationHandler name");
		} catch (ResourceInitializationException e) {}
	}
	
	@Test
	public void testConsumerInitializesHandler() throws UIMAException, IOException {
		
		// get a UimaContext containing a producer class
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(
				EmptyAnnotator.class,
				TypeSystemDescriptionFactory.createTypeSystemDescription("org.cleartk.TypeSystem"),
				SequentialInstanceConsumer.PARAM_ANNOTATION_HANDLER,
				SequentialInstanceConsumer_ImplBaseTest.Handler.class.getName());
		UimaContext context = engine.getUimaContext();
		
		// create the consumer
		Consumer consumer = new Consumer();

		// unset producer variables
		SequentialInstanceConsumer_ImplBaseTest.producer = null;
		SequentialInstanceConsumer_ImplBaseTest.producerIsInitialized = false;
		
		// initialize the consumer
		consumer.initialize(context);
		
		// make sure the producer was initialized
		Assert.assertNotNull(SequentialInstanceConsumer_ImplBaseTest.producer);
		Assert.assertTrue(SequentialInstanceConsumer_ImplBaseTest.producerIsInitialized);
	}
	
	@Test
	public void testProcessCallsHandlerAll() throws UIMAException, IOException {
		Class<? extends SequentialAnnotationHandler<Object>> producerClass = SequentialInstanceConsumer_ImplBaseTest.Handler.class;
		int instanceCount = 2;
		int consumeAllCount = 1;

		// initialize a simple AnalysisEngine
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(
				EmptyAnnotator.class,
				TypeSystemDescriptionFactory.createTypeSystemDescription("org.cleartk.TypeSystem"),
				SequentialInstanceConsumer.PARAM_ANNOTATION_HANDLER,
				producerClass.getName());
		
		// initialize the consumer
		UimaContext context = engine.getUimaContext();
		Consumer consumer = new Consumer();
		consumer.initialize(context);
		
		// set up some classification instances
		List<Instance<Object>> instances = new ArrayList<Instance<Object>>();
		for (int i = 0; i < instanceCount; i++) {
			instances.add(new Instance<Object>());
		}
		SequentialInstanceConsumer_ImplBaseTest.instances = instances;
		
		// make sure process calls the producer and that consume() or consumeAll()
		// is called the expected number of times
		consumer.process(engine.newJCas());
		Assert.assertEquals(consumeAllCount, consumer.consumeAllCount);
		
		// make sure all instances were observed
		Assert.assertEquals(instanceCount, consumer.instances.size());
		for (int i = 0; i < instanceCount; i++) {
			Assert.assertEquals(instances.get(i), consumer.instances.get(i));
		}
	}
	
	private static SequentialAnnotationHandler<Object> producer;
	private static boolean producerIsInitialized;
	private static List<Instance<Object>> instances;

}
