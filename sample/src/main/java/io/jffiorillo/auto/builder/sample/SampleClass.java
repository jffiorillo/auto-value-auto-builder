package io.jffiorillo.auto.builder.sample;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue public abstract class SampleClass {

  @NonNull abstract int edad();
  @NonNull abstract String name();
  @NonNull abstract String motherName();
  @NonNull abstract String fatherName();
  @NonNull abstract boolean isDead();

  abstract String dirrection();
}
