package com.gradle.info;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.join;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "mojo-inspection")
public class MojoInspectionMojo extends AbstractMavenLifecycleParticipant {

    private final PlexusContainer container;
    private final BuildPluginManager pluginManager;
    private final Map<File, String> hashCache = new HashMap<>();

    @Inject
    public MojoInspectionMojo(PlexusContainer container, BuildPluginManager pluginManager) {
        this.container = container;
        this.pluginManager = pluginManager;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        BuildScanApi buildScan = ApiAccessor.lookupBuildScanApi(container, getClass());

        if (buildScan != null) {
            for (MavenProject project : session.getAllProjects()) {
                List<Plugin> aspectjPlugins = project.getPluginManagement().getPlugins().stream()
                    .filter(plugin -> plugin.getGroupId().equals("org.codehaus.mojo")
                        && plugin.getArtifactId().equals("aspectj-maven-plugin"))
                    .collect(Collectors.toList());

                for (Plugin plugin : aspectjPlugins) {
                    try {
                        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(plugin, project.getRemotePluginRepositories(), session.getRepositorySession());
                        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo("compile");
                        if (mojoDescriptor != null) {
                            Class<?> implementationClass = pluginManager.getPluginRealm(session, pluginDescriptor).loadClass(mojoDescriptor.getImplementation());
                            URLClassLoader classLoader = (URLClassLoader) implementationClass.getClassLoader();
                            String classpathList = join(stream(classLoader.getURLs()).map(this::getHashString).collect(Collectors.toList()), '\n');
                            buildScan.value(project.getName() + ":" + mojoDescriptor.getId(), classpathList);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private String getHashString(URL url) {
        try {
            String hash;
            File file = Paths.get(url.toURI()).toFile();
            if (hashCache.containsKey(file)) {
                hash = hashCache.get(file);
            } else {
                hash = file.getAbsolutePath() + " " + DigestUtils.md5Hex(new FileInputStream(file));
                hashCache.put(file, hash);
            }
            return hash;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
