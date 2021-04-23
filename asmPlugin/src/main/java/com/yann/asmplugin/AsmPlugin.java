package com.yann.asmplugin;


import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AsmPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        AppExtension appExtension = target.getExtensions().getByType(AppExtension.class);
        appExtension.registerTransform(new AsmLifeCycleTransform("fdsfdsfsdfsd"));
    }
}