package com.frengor;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Resolves the version of the mappings used by the provided Spigot server.
 */
@Mojo(name = "resolve-mappings-version", defaultPhase = LifecyclePhase.INITIALIZE)
public class MappingsVersionResolver extends AbstractMojo {

    private static final Pattern CRAFT_MAGIC_NUMBERS = Pattern.compile("org/bukkit/craftbukkit/v\\d+_\\d+_R\\d+/util/CraftMagicNumbers\\.class");

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The path to the server's Jar file or its artifact as a string in the form of groupId:artifactId:version[:type[:classifier]]
     * (the default type is "jar").
     */
    @Parameter(property = "server", required = true)
    private String server;

    /**
     * The property in which to put the resolved mappings version.
     */
    @Parameter(defaultValue = "resolvedMappingsVersion", property = "outputProperty")
    private String outputProperty;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", required = true, readonly = true)
    private List<RemoteRepository> remoteRepositories;

    public void execute() throws MojoExecutionException {
        File serverJar;

        if (server.contains(":")) {
            try {
                serverJar = resolveArtifact(server);
            } catch (ArtifactResolutionException e) {
                getLog().error(e);
                throw new MojoExecutionException("Couldn't resolve artifact " + server + ": " + e.getMessage(), e);
            }
        } else {
            serverJar = new File(server);
            if (!serverJar.isAbsolute()) {
                serverJar = new File(project.getBasedir(), server);
            }
        }

        if (!serverJar.exists()) {
            throw new MojoExecutionException("File doesn't exists!");
        }

        try (ZipFile file = new ZipFile(serverJar)) {
            ZipEntry craftBukkitDir = file.getEntry("org/bukkit/craftbukkit");

            if (!craftBukkitDir.isDirectory()) {
                throw new MojoExecutionException("Couldn't find the org.bukkit.craftbukkit package inside " + server);
            }

            List<ZipEntry> entries = file.stream()
                    .filter(entry -> !entry.isDirectory() && CRAFT_MAGIC_NUMBERS.matcher(entry.getName()).matches())
                    .collect(Collectors.toList());

            if (entries.isEmpty()) {
                throw new MojoExecutionException("Couldn't find the CraftMagicNumbers class inside " + server);
            }
            if (entries.size() > 1) {
                getLog().debug("Found too many CraftMagicNumbers classes inside " + server);
                entries.forEach(entry -> getLog().debug(entry.getName()));
                throw new MojoExecutionException("Found multiple CraftMagicNumbers classes inside " + server);
            }

            ZipEntry craftMagicNumbers = entries.get(0);

            CraftMagicNumbersVisitor visitor = new CraftMagicNumbersVisitor(Opcodes.ASM9);
            new ClassReader(file.getInputStream(craftMagicNumbers)).accept(visitor, ClassReader.SKIP_DEBUG);

            String resolvedVersion = visitor.getVersion();

            if (resolvedVersion == null) {
                throw new MojoExecutionException("Couldn't find or get the mappings version from method getMappingsVersion()");
            }

            project.getProperties().setProperty(outputProperty, resolvedVersion);
        } catch (MojoExecutionException e) {
            getLog().error(e);
            throw e; // Just re-throw
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Couldn't resolve the mappings version: " + e.getMessage(), e);
        }
    }

    private File resolveArtifact(String coordinates) throws MojoExecutionException, ArtifactResolutionException {
        String[] array = coordinates.split(":");
        if (array.length < 3 || array.length > 5) {
            throw new MojoExecutionException("Invalid server artifact, it must be in the form groupId:artifactId:version[:type[:classifier]] " + coordinates);
        }
        String groupId = array[0];
        String artifactId = array[1];
        String version = array[2];
        String type = "jar";
        String classifier = null;

        if (array.length > 3) {
            type = array[3];
            if (array.length > 4) {
                classifier = array[4];
            }
        }

        Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);

        ArtifactRequest remoteRequest = new ArtifactRequest().setRepositories(remoteRepositories).setArtifact(artifact);
        ArtifactResult remoteResult = repoSystem.resolveArtifact(repoSession, remoteRequest);
        return remoteResult.getArtifact().getFile();
    }
}
