package io.jffiorillo.auto.builder;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueAutoBuilderExtension extends AutoValueExtension {

  private static final String GENERATED_COMMENTS =
      "https://github" + ".com/jffiorillo/auto-value-auto-builder";

  private static final AnnotationSpec GENERATED =
      AnnotationSpec.builder(Generated.class)
          .addMember("value", "$S",
              AutoValueAutoBuilderExtension.class.getName())
          .addMember("comments", "$S", GENERATED_COMMENTS)
          .build();

  @Override public String generateClass(Context context, String className,
      String classToExtend, boolean isFinal) {

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    final TypeElement typeElement = context.autoValueClass();
    TypeName superClassType =
        ClassName.get(context.packageName(), classToExtend);
    ClassName autoValueClass = ClassName.get(typeElement);
    boolean generatedAnnotationAvailable = context.processingEnvironment()
        .getElementUtils()
        .getTypeElement("javax.annotation.Generated") != null;
    List<Property> properties = readProperties(context.properties());

    List<? extends TypeParameterElement> typeParams =
        context.autoValueClass().getTypeParameters();
    List<TypeVariableName> params = new ArrayList<>(typeParams.size());
    Map<String, TypeName> types =
        convertPropertiesToTypes(context.properties());

    final TypeSpec.Builder classBuild =
        generateCreateFunction(properties, classNameClass,
            autoValueClass, classToExtend).superclass(superClassType)
            .addMethod(generateConstructor(properties, types));

    return JavaFile.builder(context.packageName(), classBuild.build())
        .build()
        .toString();
  }

  private TypeSpec.Builder generateCreateFunction(List<Property> properties,
      ClassName className, ClassName autoValueClass, String classToExtend) {
    final MethodSpec.Builder createMethod = MethodSpec.methodBuilder("create")
        .addModifiers(PUBLIC, STATIC)
        .addException(NullPointerException.class)
        .returns(autoValueClass);

    for (Property property : properties) {
      final ParameterSpec.Builder parameter =
          ParameterSpec.builder(property.type, property.humanName);
      createMethod.addParameter(parameter.build());
    }
    addReturnStatement(properties, autoValueClass, createMethod);

    return TypeSpec.classBuilder(className).addMethod(createMethod.build());
  }

  void addReturnStatement(List<Property> properties, ClassName autoValueClass,
      MethodSpec.Builder createMethod) {
    StringBuilder returnStatement = new StringBuilder(
        "return new AutoValue_" + autoValueClass.simpleName() + "(");

    Map<Property, FieldSpec> fields = new LinkedHashMap<>(properties.size());
    Iterator<Property> iterator = properties.iterator();
    while (iterator.hasNext()) {
      Property property = iterator.next();
      TypeName fieldType = property.type;
      FieldSpec field =
          FieldSpec.builder(fieldType, property.humanName).build();
      fields.put(property, field);
      returnStatement.append("$N");
      if (iterator.hasNext()) returnStatement.append(", ");
    }
    returnStatement.append(")");
    createMethod.addStatement(returnStatement.toString(),
        fields.values().toArray());
  }

  MethodSpec generateConstructor(List<Property> properties,
      Map<String, TypeName> types) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Property property : properties) {
      ParameterSpec.Builder builder =
          ParameterSpec.builder(property.type, property.humanName);
      if (property.nullable()) {
        builder.addAnnotation(
            ClassName.bestGuess(property.nullableAnnotation()));
      }
      params.add(builder.build());
    }

    MethodSpec.Builder builder =
        MethodSpec.constructorBuilder().addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    for (int i = properties.size(); i > 0; i--) {
      superFormat.append("$N");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), types.keySet().toArray());

    return builder.build();
  }

  /**
   * Converts the ExecutableElement properties to TypeName properties
   */
  Map<String, TypeName> convertPropertiesToTypes(
      Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      ExecutableElement el = entry.getValue();
      types.put(entry.getKey(), TypeName.get(el.getReturnType()));
    }
    return types;
  }

  public List<Property> readProperties(
      Map<String, ExecutableElement> properties) {
    List<Property> values = new LinkedList<Property>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values;
  }

  @Override public boolean applicable(Context context) {
    return true;
  }

  public static class Property {
    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final ImmutableSet<String> annotations;
    final boolean nullable;

    public Property(String humanName, ExecutableElement element) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = humanName;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);
      nullable = nullableAnnotation() != null;
    }

    public static TypeMirror getAnnotationValue(Element foo,
        Class<?> annotation) {
      AnnotationMirror am = getAnnotationMirror(foo, annotation);
      if (am == null) {
        return null;
      }
      AnnotationValue av = getAnnotationValue(am, "value");
      return av == null ? null : (TypeMirror) av.getValue();
    }

    private static AnnotationMirror getAnnotationMirror(Element typeElement,
        Class<?> clazz) {
      String clazzName = clazz.getName();
      for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
        if (m.getAnnotationType().toString().equals(clazzName)) {
          return m;
        }
      }
      return null;
    }

    private static AnnotationValue getAnnotationValue(
        AnnotationMirror annotationMirror, String key) {
      Map<? extends ExecutableElement, ? extends AnnotationValue> values =
          annotationMirror.getElementValues();
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values
          .entrySet()) {
        if (entry.getKey().getSimpleName().toString().equals(key)) {
          return entry.getValue();
        }
      }
      return null;
    }

    public boolean nullable() {
      return nullable;
    }

    public String nullableAnnotation() {
      for (String annotationString : annotations) {
        if (annotationString.equals("@Nullable") || annotationString.endsWith(
            ".Nullable")) {
          return annotationString;
        }
      }
      return null;
    }

    private ImmutableSet<String> buildAnnotations(ExecutableElement element) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();

      for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
        builder.add(annotation.getAnnotationType().asElement().toString());
      }

      return builder.build();
    }
  }
}
