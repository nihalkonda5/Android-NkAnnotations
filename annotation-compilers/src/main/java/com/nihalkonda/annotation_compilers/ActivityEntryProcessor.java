package com.nihalkonda.annotation_compilers;

import com.google.auto.service.AutoService;
import com.nihalkonda.annotations.ActivityEntry;
import com.nihalkonda.annotations.ActivityEntryData;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class ActivityEntryProcessor extends AbstractProcessor {

    private static final String TAG = "ActivityEntryProcessor";

    private Elements elementUtils; //  Tool classes that manipulate elements
    private Filer filer;  //  Used to create files
    private Messager messager; //  Used to output logs, errors, or warnings
    ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.elementUtils = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ActivityEntry.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(ActivityEntry.class)) {
            log("Element: "+element.toString());
            String className = element.getSimpleName().toString()+"Entry";
            TypeSpec.Builder builder = TypeSpec
                    .classBuilder(className)
                    .addModifiers(Modifier.PUBLIC);

            MethodSpec enterSimply = MethodSpec.methodBuilder("startActivity")
                    .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get("android.app", "Activity"),"activity")
                    .addStatement("activity.startActivity(new android.content.Intent(activity,"+element.getSimpleName().toString()+".class))")
                    .build();

            boolean addSimpleEntry = true;

            for(Element e : element.getEnclosedElements()){
                log(e.toString());
                ActivityEntryData activityEntryData = e.getAnnotation(ActivityEntryData.class);
                if(activityEntryData!=null){
                    MethodSpec enterWithData = MethodSpec.methodBuilder("startActivity")
                            .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
                            .returns(TypeName.VOID)
                            .addParameter(ClassName.get("android.app", "Activity"),"activity")
                            .addParameter(ClassName.get(e.asType()),e.toString())
                            .addStatement("android.content.Intent intent = new android.content.Intent(activity,"+element.getSimpleName().toString()+".class)")
                            .addStatement("intent.putExtra(\""+e.toString()+"\","+e.toString()+")")
                            .addStatement("activity.startActivity(intent)")
                            .build();
                    builder.addMethod(enterWithData);
                    if(activityEntryData.value())
                        addSimpleEntry = false;
                }
            }

            if(addSimpleEntry)
                builder.addMethod(enterSimply);

            TypeSpec typeSpec = builder.build();
            //Specify package name
            PackageElement packageElement = this.elementUtils.getPackageOf(element);
            log("Element: "+packageElement.getQualifiedName().toString());
            try {
                JavaFile.builder(packageElement.getQualifiedName().toString(), typeSpec).build().writeTo(this.filer);
            } catch (IOException e) {
                e.printStackTrace();
                log(e.toString());
            }
        }
        return true;
    }

    public void log(String s){
        System.out.println(TAG+" : "+s);
        this.messager.printMessage(Diagnostic.Kind.NOTE,TAG+" : "+s);
    }

}