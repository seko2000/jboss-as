/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.host.controller.DomainModelProxy;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.dmr.ModelNode;

/**
 * The handler to add the local host definition to the DomainModel.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LocalHostAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-host";

    private final ModelNodeRegistration registration;

    private final DomainModelProxy domainModelProxy;

    public static LocalHostAddHandler getInstance(final ModelNodeRegistration registration, final DomainModelProxy domainModelProxy) {
        return new LocalHostAddHandler(registration, domainModelProxy);
    }

    private LocalHostAddHandler(ModelNodeRegistration registration, DomainModelProxy domainModelProxy) {
        this.registration = registration;
        this.domainModelProxy = domainModelProxy;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO - Create and return the ModelDescription
        return new ModelNode();
    }

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {
        try {
            final ModelNode model = context.getSubModel();
            HostModelUtil.initCoreModel(model);

            String hostName = operation.require(NAME).asString();
            model.get(NAME).set(hostName);

            // If this is not in an environment where a RuntimeContext is supplied then the actual DomainModel does not need
            // to be updated as that is runtime state.
            if (context.getRuntimeContext() != null) {
                domainModelProxy.getDomainModel().setLocalHostName(hostName);
            }
            context.getRegistry().registerSubModel(PathElement.pathElement(HOST, hostName), registration);

            ModelNode compensating = Util.getResourceRemoveOperation(operation.get(OP_ADDR));
            resultHandler.handleResultComplete();
            return new BasicOperationResult(compensating);
        } catch (Exception e) {
            throw new OperationFailedException(new ModelNode().set(e.getLocalizedMessage()));
        }
    }
}