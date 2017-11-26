package io.jffiorillo.auto.builder.sample;

import org.junit.Test;

import static org.junit.Assert.*;

public class SampleClassTest {


  @Test public void value() throws Exception {

    assertNotNull(AutoValue_SampleClass.create("a"));
  }
}