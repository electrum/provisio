package io.tesla.proviso.archive;

import io.provis.model.ProvisioContext;
import io.provis.model.RuntimeEntry;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;

import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTestCase;

public class ArchiveTest extends InjectedTestCase {

  @Inject
  @Named("${basedir}/target/test.jar")
  private File jar;

  @Inject
  @Named("${basedir}/target/output")
  private File archiveOutputDirectory;
  
  @Inject
  @Named("${basedir}/src/test/sources")
  private File archiveResources;

  @Inject
  @Named("${basedir}/src/test/archives")
  private File archives;

  private Archiver archiver;
  
  protected void setUp() throws Exception {
    super.setUp();
    
    archiver = new DefaultArchiver();
  }

  public void testPreservationOfFileModes() throws Exception {
    
    FileUtils.deleteDirectory(archiveOutputDirectory);
    
    // Unpack a tar.gz
    File archive = new File(archives, "apache-maven-3.0.4-bin.tar.gz");
    Map<String, RuntimeEntry> fileEntries = archiver.unarchive(archive, archiveOutputDirectory);
    assertTrue(FileMode.EXECUTABLE_FILE.equals(fileEntries.get("apache-maven-3.0.4/bin/mvn").getMode()));
    assertTrue(FileMode.EXECUTABLE_FILE.equals(fileEntries.get("apache-maven-3.0.4/bin/mvnDebug").getMode()));
    assertTrue(FileMode.EXECUTABLE_FILE.equals(fileEntries.get("apache-maven-3.0.4/bin/mvnyjp").getMode()));
    
    // Pack a tar.gz
    ProvisioContext context = new ProvisioContext();
    context.setFileEntries(fileEntries);
    File sourceDirectory = new File(archiveOutputDirectory, "apache-maven-3.0.4");
    File reArchive = new File(archiveOutputDirectory, "apache-maven-3.0.4.tar.gz");
    archiver.archive(reArchive, sourceDirectory, context);
    
    FileUtils.deleteDirectory(sourceDirectory);
    
    File archive1 = new File(archiveOutputDirectory, "apache-maven-3.0.4.tar.gz");
    Map<String, RuntimeEntry> fileEntries1 = archiver.unarchive(archive1, archiveOutputDirectory);
    assertTrue(FileMode.EXECUTABLE_FILE.equals(fileEntries1.get("apache-maven-3.0.4/bin/mvn").getMode()));
    assertTrue(FileMode.EXECUTABLE_FILE.equals(fileEntries1.get("apache-maven-3.0.4/bin/mvnDebug").getMode()));
    assertTrue(FileMode.EXECUTABLE_FILE.equals(fileEntries1.get("apache-maven-3.0.4/bin/mvnyjp").getMode()));

    
  }
  
  protected void assertPresenceOfEntryInJar(String entryName, File archive) throws IOException {
    boolean returnValue;
    JarFile jarFile = new JarFile(archive);
    JarEntry entry = jarFile.getJarEntry(entryName);
    if(entry != null) {
      returnValue = true;
    } else {
      returnValue = false;      
    }
    Assert.assertTrue("The jarEntry '" + entryName + "' is expected to be present, but is not.", returnValue);
  }
}
