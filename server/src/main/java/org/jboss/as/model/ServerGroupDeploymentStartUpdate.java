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

package org.jboss.as.model;

import org.jboss.as.server.deployment.ServerModelDeploymentStartUpdate;


/**
 * Update used when updating a deployment element to be started or stopped.
 *
 * @author John E. Bailey
 * @author Brian Stansberry
 */
public class ServerGroupDeploymentStartUpdate extends AbstractModelUpdate<ServerGroupElement, Void> {
    private static final long serialVersionUID = 5773083013951607950L;

    private final String deploymentUnitName;
    private final String deploymentRuntimeName;
    private final byte[] deploymentHash;

    public ServerGroupDeploymentStartUpdate(final String deploymentUnitName, final String deploymentRuntimeName, final byte[] deploymentHash) {
        if (deploymentUnitName == null)
            throw new IllegalArgumentException("deploymentUnitName is null");
        this.deploymentUnitName = deploymentUnitName;
        this.deploymentRuntimeName = deploymentRuntimeName;
        this.deploymentHash = deploymentHash;
    }

      @Override
    public AbstractModelUpdate<ServerGroupElement, ?> getCompensatingUpdate(ServerGroupElement original) {
        return new ServerGroupDeploymentStopUpdate(deploymentUnitName);
    }

    @Override
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerModelDeploymentStartUpdate(deploymentUnitName, deploymentRuntimeName, deploymentHash);
    }

    @Override
    protected void applyUpdate(ServerGroupElement serverGroupElement) throws UpdateFailedException {
        ServerGroupDeploymentElement deploymentElement = serverGroupElement.getDeployment(deploymentUnitName);
        if (deploymentElement == null) {
            throw new UpdateFailedException(String.format("Deployment %s does not exist in server group %s", deploymentElement, serverGroupElement.getName()));
        }
        deploymentElement.setStart(true);
    }

    @Override
    public Class<ServerGroupElement> getModelElementType() {
        return ServerGroupElement.class;
    }
}