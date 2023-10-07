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

import java.io.File;
import java.util.List;

/**
 * Resolves the version of the mappings used by the provided Spigot server.
 */
@Mojo(name = "resolve-mappings-version", defaultPhase = LifecyclePhase.INITIALIZE)
public class MappingsVersionResolver extends AbstractMojo {

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The path to the server's Jar file or its artifact as a string in the form of groupId:artifactId:version[:type[:classifier]].
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

        project.getProperties().setProperty(outputProperty, serverJar.getAbsolutePath());
    }

    private File resolveArtifact(String coordinates) throws MojoExecutionException, ArtifactResolutionException {
        String[] array = coordinates.split(":");
        if (array.length < 3 || array.length > 5) {
            throw new MojoExecutionException("Invalid server artifact, it must be in the form groupId:artifactId:version[:type[:classifier]] " + coordinates);
        }
        String groupId = array[0];
        String artifactId = array[1];
        String version = array[2];
        String type = null;
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
