/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server.deployment.module;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileVisitor;
import org.jboss.vfs.VisitorAttributes;

/**
 * Processor responsible for discovering nested jars and mounting/attaching them to the deployment
 *
 * @author Jason T. Greene
 */
public class NestedJarInlineProcessor implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");

    /**
     * Mounts all nested jars inline with the mount of the deployment jar.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ResourceRoot deploymentRoot = phaseContext.getDeploymentUnitContext().getAttachment(Attachments.DEPLOYMENT_ROOT);
        final List<VirtualFile> list = new ArrayList<VirtualFile>(1);
        try {
            deploymentRoot.getRoot().visit(new VirtualFileVisitor() {
                public void visit(VirtualFile virtualFile) {
                    if (virtualFile.getName().endsWith(".jar")) {
                        list.add(virtualFile);
                    }
                }
                public VisitorAttributes getAttributes() {
                    return VisitorAttributes.RECURSE_LEAVES_ONLY;
                }
            });
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException("Could not mount nested jars in deployment: " + phaseContext.getDeploymentUnitContext().getName(), e);
        }

        if (list.size() == 0)
            return;

        final NestedMounts mounts = new NestedMounts(list.size());
        for (VirtualFile file : list)
        try {
            MountHandle handle = new MountHandle(VFS.mountZip(file, file, TempFileProviderService.provider()));
            mounts.add(file, handle);
        } catch (IOException e) {
            log.warnf("Could not mount %s in deployment %s, skipping", file.getPathNameRelativeTo(deploymentRoot), deploymentRoot.getName());
        }

        phaseContext.getDeploymentUnitContext().putAttachment(NestedMounts.ATTACHMENT_KEY, mounts);
    }

    public void undeploy(final DeploymentUnit context) {
        final NestedMounts nestedMounts = context.removeAttachment(NestedMounts.ATTACHMENT_KEY);
        if (nestedMounts != null) {
            VFSUtils.safeClose(nestedMounts.getClosables());
        }
    }

    static class CloseListener extends AbstractServiceListener<Void> {
        private Closeable[] closeables;

        CloseListener(Closeable[] closeables) {
            this.closeables = closeables;
        }

        @Override
        public void serviceStopped(ServiceController<? extends Void> controller) {
            if (closeables != null) {
                for (Closeable close : closeables) {
                    try {
                        close.close();
                    } catch (IOException e) {
                        // Munch munch
                    }
                }
                closeables = null;
            }
        }
    }
}