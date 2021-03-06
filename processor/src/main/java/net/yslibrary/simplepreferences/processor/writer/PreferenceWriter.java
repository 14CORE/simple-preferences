package net.yslibrary.simplepreferences.processor.writer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import net.yslibrary.simplepreferences.processor.PreferenceAnnotatedClass;

/**
 * Created by yshrsmz on 2016/02/23.
 */
public class PreferenceWriter {

  private final PreferenceAnnotatedClass annotatedClass;

  public PreferenceWriter(PreferenceAnnotatedClass annotatedClass) {
    this.annotatedClass = annotatedClass;
  }

  public TypeSpec write() {
    boolean useDefaultPreferences = annotatedClass.useDefaultPreferences();
    String preferenceName = annotatedClass.preferenceName;
    String preferenceClassName = annotatedClass.preferenceClassName;
    String packageName = annotatedClass.packageName;
    TypeElement annotatedElement = annotatedClass.annotatedElement;

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(preferenceClassName)
        .addModifiers(Modifier.PUBLIC)
        .superclass(ClassName.get(annotatedElement));

    TypeName generatingClass = ClassName.get(packageName, preferenceClassName);

    // SharedPreferences field ---
    FieldSpec prefsField =
        FieldSpec.builder(SharedPreferences.class, "prefs", Modifier.PRIVATE, Modifier.FINAL)
            .build();
    classBuilder.addField(prefsField);

    // constructor
    classBuilder.addMethod(writeConstructor(preferenceName, useDefaultPreferences));

    // create method
    classBuilder.addMethod(writeFactory(generatingClass));

    // clear method
    classBuilder.addMethod(writeClear());

    if (annotatedClass.needCommitMethodForClear) {
      classBuilder.addMethod(writeClearWithCommit());
    }

    // keys
    annotatedClass.keys.forEach(keyAnnotatedField -> {
      List<MethodSpec> methods =
          TypeWriter.create(generatingClass, keyAnnotatedField).writeMethods(prefsField);

      classBuilder.addMethods(methods);
    });

    return classBuilder.build();
  }


  private MethodSpec writeConstructor(String preferenceName, boolean useDefaultPreferences) {
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addParameter(
            ParameterSpec.builder(Context.class, "context").addAnnotation(NonNull.class).build());

    if (useDefaultPreferences) {
      constructorBuilder.addStatement(
          "prefs = $T.getDefaultSharedPreferences(context.getApplicationContext())",
          PreferenceManager.class);
    } else {
      constructorBuilder.addStatement(
          "prefs = context.getApplicationContext().getSharedPreferences($S, Context.MODE_PRIVATE)",
          preferenceName);
    }

    return constructorBuilder.build();
  }

  private MethodSpec writeFactory(TypeName generatingClass) {
    MethodSpec.Builder createMethod = MethodSpec.methodBuilder("create")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(
            ParameterSpec.builder(Context.class, "context").addAnnotation(NonNull.class).build())
        .returns(generatingClass);

    createMethod.beginControlFlow("if (context == null)")
        .addStatement("throw new NullPointerException($S)", "Context is Null!")
        .endControlFlow();
    createMethod.addStatement("return new $T(context)", generatingClass);

    return createMethod.build();
  }

  private MethodSpec writeClear() {
    MethodSpec.Builder clearMethod = MethodSpec.methodBuilder("clear")
        .addModifiers(Modifier.PUBLIC);
    clearMethod.addStatement("prefs.edit().clear().apply()");

    return clearMethod.build();
  }

  private MethodSpec writeClearWithCommit() {
    MethodSpec.Builder clearWithCommitMethod = MethodSpec.methodBuilder("clearWithCommit")
        .addModifiers(Modifier.PUBLIC)
        .addStatement("prefs.edit().clear().commit()");

    return clearWithCommitMethod.build();
  }
}
