/*
 *     Copyright 2021 https://dnation.cloud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cloud.dnation.jenkins.plugins.hetzner;

import cloud.dnation.jenkins.plugins.hetzner.launcher.AbstractHetznerSshConnector;
import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jenkinsci.plugins.cloudstats.ProvisioningActivity;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.util.Set;

import static cloud.dnation.jenkins.plugins.hetzner.ConfigurationValidator.*;
import static cloud.dnation.jenkins.plugins.hetzner.ConfigurationValidator.doCheckNonEmpty;
import static cloud.dnation.jenkins.plugins.hetzner.ConfigurationValidator.doCheckPositiveInt;

@ToString
@Slf4j
public class HetznerServerTemplate extends AbstractDescribableImpl<HetznerServerTemplate> {
    @Getter
    private final String name;

    @Getter
    private final String labelStr;

    @Getter
    private final String image;

    @Getter
    private final String location;

    @Getter
    private final String serverType;

    @Getter
    private transient Set<LabelAtom> labels;

    @Setter(AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    @NonNull
    private transient HetznerCloud cloud;

    @Setter(onMethod = @__({@DataBoundSetter}))
    @Getter
    private AbstractHetznerSshConnector connector;

    @Setter(onMethod = @__({@DataBoundSetter}))
    @Getter
    private String remoteFs;

    @Setter(onMethod = @__({@DataBoundSetter}))
    @Getter
    private String jvmOpts;

    @Setter(onMethod = @__({@DataBoundSetter}))
    @Getter
    private String keepAroundMinutes;

    @Getter
    @Setter(onMethod = @__({@DataBoundSetter}))
    private int numExecutors;

    @Getter
    @Setter(onMethod = @__({@DataBoundSetter}))
    private int bootDeadline;

    @DataBoundConstructor
    public HetznerServerTemplate(String name, String labelStr, String image,
                                 String location, String serverType) throws FormValidation {
        this.name = name;
        this.labelStr = Util.fixNull(labelStr);
        this.image = image;
        this.location = location;
        this.serverType = serverType;
        readResolve();
    }

    protected Object readResolve() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        labels = Label.parse(labelStr);
        if (Strings.isNullOrEmpty(location)) {
            throw new IllegalArgumentException("Location must be specified");
        }
        if (numExecutors == 0) {
            setNumExecutors(HetznerConstants.DEFAULT_NUM_EXECUTORS);
        }
        if (bootDeadline == 0) {
            setBootDeadline(HetznerConstants.DEFAULT_BOOT_DEADLINE);
        }
        return this;
    }

    /**
     * Create new {@link HetznerServerAgent}.
     *
     * @param provisioningId ID to track activity of provisioning
     * @param nodeName       name of server
     * @return new agent instance
     */
    HetznerServerAgent createAgent(ProvisioningActivity.Id provisioningId, String nodeName)
            throws IOException, Descriptor.FormException {
        return new HetznerServerAgent(
                provisioningId,
                nodeName, remoteFs, connector.createLauncher(),
                cloud,
                this
        );
    }

    @SuppressWarnings("unused")
    @Extension
    public static final class DescriptorImpl extends Descriptor<HetznerServerTemplate> {
        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.serverTemplate_displayName();
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doVerifyLocation(@QueryParameter String location,
                                               @QueryParameter String credentialsId) {
            return verifyLocation(location, credentialsId).toFormValidation();
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doVerifyImage(@QueryParameter String image,
                                            @QueryParameter String credentialsId) {
            return verifyImage(image, credentialsId).toFormValidation();
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doVerifyServerType(@QueryParameter String serverType,
                                                 @QueryParameter String credentialsId) {
            return verifyServerType(serverType, credentialsId).toFormValidation();
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doCheckImage(@QueryParameter String image) {
            return doCheckNonEmpty(image, "Image");
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doCheckLabelStr(@QueryParameter String labelStr) {
            return doCheckNonEmpty(labelStr, "Label");
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doCheckServerType(@QueryParameter String serverType) {
            return doCheckNonEmpty(serverType, "Server type");
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doCheckLocation(@QueryParameter String location) {
            return doCheckNonEmpty(location, "Location");
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doCheckName(@QueryParameter String name) {
            return doCheckNonEmpty(name, "Name");
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doCheckNumExecutors(@QueryParameter String numExecutors) {
            return doCheckPositiveInt(numExecutors, "Number of executors");
        }

        @Restricted(NoExternalUse.class)
        @RequirePOST
        public FormValidation doCheckBootDeadline(@QueryParameter String bootDeadline) {
            return doCheckPositiveInt(bootDeadline, "Boot deadline");
        }
    }
}
