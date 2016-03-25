/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.maven.plugins.provisio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.FileUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.provis.Actions;
import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;

@Named
@Singleton
public class Provisio {

  private final ArtifactHandlerManager artifactHandlerManager;

  @Inject
  public Provisio(ArtifactHandlerManager artifactHandlerManager) {
    this.artifactHandlerManager = artifactHandlerManager;
  }

  public List<Runtime> findDescriptors(File descriptorDirectory, MavenProject project) {
    List<Runtime> runtimes = Lists.newArrayList();
    runtimes.addAll(findDescriptorsInFileSystem(descriptorDirectory, project));
    runtimes.addAll(findDescriptorsForPackagingTypeInExtensionRealms(project));
    return runtimes;
  }

  public List<Runtime> findDescriptorsInFileSystem(File descriptorDirectory, MavenProject project) {
    List<Runtime> runtimes = Lists.newArrayList();
    if (descriptorDirectory.exists()) {
      try {
        List<File> descriptors = FileUtils.getFiles(descriptorDirectory, "*.xml", null);
        for (File descriptor : descriptors) {
          Runtime runtime = parseDescriptor(new FileInputStream(descriptor), project);
          runtimes.add(runtime);
        }
      } catch (IOException e) {
        throw new RuntimeException(String.format("Error parsing provisioning descriptors in %s.", descriptorDirectory), e);
      }
    }
    return runtimes;
  }

  public List<Runtime> findDescriptorsForPackagingTypeInExtensionRealms(MavenProject project) {
    List<Runtime> runtimes = Lists.newArrayList();
    if (project.getClassRealm() != null) {
      Collection<ClassRealm> extensionRealms = project.getClassRealm().getImportRealms();
      if (extensionRealms != null) {
        for (ClassRealm extensionRealm : extensionRealms) {
          String descriptorResourceLocation = String.format("META-INF/provisio/%s.xml", project.getPackaging());
          InputStream inputStream = extensionRealm.getResourceAsStream(descriptorResourceLocation);
          if (inputStream != null) {
            Runtime runtime = parseDescriptor(inputStream, project);
            runtimes.add(runtime);
          }
        }
      }
    }
    return runtimes;
  }

  public Runtime parseDescriptor(InputStream inputStream, MavenProject project) {
    RuntimeReader parser = new RuntimeReader(Actions.defaultActionDescriptors(), versionMap(project));
    Map<String, String> variables = Maps.newHashMap();
    variables.putAll((Map) project.getProperties());
    variables.put("project.version", project.getVersion());
    variables.put("project.groupId", project.getGroupId());
    variables.put("project.artifactId", project.getArtifactId());
    variables.put("basedir", project.getBasedir().getAbsolutePath());
    return parser.read(inputStream, variables);
  }

  //
  // The version map to use when versions are not specified for the artifacts in the assembly/runtime document.
  //
  private Map<String, String> versionMap(MavenProject project) {
    Map<String, String> versionMap = Maps.newHashMap();
    DependencyManagement dependencyManagement = project.getDependencyManagement();
    if (dependencyManagement != null) {
      for (Dependency managedDependency : project.getDependencyManagement().getDependencies()) {
        String versionlessCoordinate = toVersionlessCoordinate(managedDependency);
        versionMap.put(versionlessCoordinate, managedDependency.getVersion());
      }
    }
    //
    // Add a map entry for the project itself in the event that its not in dependency management so that users
    // don't have to put versions in the descriptor when including the project being built.
    //
    versionMap.put(toVersionlessCoordinate(project), project.getVersion());

    return versionMap;
  }

  public List<String> getManagedDependencies(MavenProject project) {
    List<String> managedDependencies = Lists.newArrayList();
    DependencyManagement dependencyManagement = project.getDependencyManagement();
    if (dependencyManagement != null) {
      for (Dependency managedDependency : project.getDependencyManagement().getDependencies()) {
        managedDependencies.add(toCoordinate(managedDependency));
      }
    }
    return managedDependencies;
  }

  public String toCoordinate(Dependency d) {
    StringBuffer sb = new StringBuffer().append(d.getGroupId()).append(":").append(d.getArtifactId()).append(":").append(d.getType());
    if (d.getClassifier() != null && d.getClassifier().isEmpty() == false) {
      sb.append(":").append(d.getClassifier());
    }
    sb.append(":").append(d.getVersion());
    return sb.toString();
  }

  public String toVersionlessCoordinate(Dependency d) {
    StringBuffer sb = new StringBuffer().append(d.getGroupId()).append(":").append(d.getArtifactId()).append(":").append(d.getType());
    if (d.getClassifier() != null && d.getClassifier().isEmpty() == false) {
      sb.append(":").append(d.getClassifier());
    }
    return sb.toString();
  }

  public String toVersionlessCoordinate(MavenProject project) {
    String extension = artifactHandlerManager.getArtifactHandler(project.getPackaging()).getExtension();
    StringBuffer sb = new StringBuffer().append(project.getGroupId()).append(":").append(project.getArtifactId()).append(":").append(extension);
    return sb.toString();
  }

}
