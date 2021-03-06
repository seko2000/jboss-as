/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.common;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the interface resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InterfaceAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddInterfaceOperation(ModelNode address, ModelNode criteria) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        op.get(CRITERIA).set(criteria);
        return op;
    }

    public static final InterfaceAddHandler NAMED_INSTANCE = new InterfaceAddHandler(false);

    public static final InterfaceAddHandler SPECIFIED_INSTANCE = new InterfaceAddHandler(true);

    private final boolean specified;

    /**
     * Create the InterfaceAddHandler
     */
    protected InterfaceAddHandler(boolean specified) {
        this.specified = specified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        ModelNode model = context.getSubModel();
        model.get(NAME).set(name);

        ModelNode criteriaNode = operation.get(CRITERIA);
        ParsedInterfaceCriteria parsed = ParsedInterfaceCriteria.parse(criteriaNode.clone(), specified);
        if (parsed.getFailureMessage() != null) {
            throw new OperationFailedException(new ModelNode().set(parsed.getFailureMessage()));
        }
        model.get(CRITERIA).set(criteriaNode);
        ModelNode compensating = Util.getResourceRemoveOperation(operation.get(OP_ADDR));
        return installInterface(name, parsed, context, resultHandler, compensating);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return specified ? InterfaceDescription.getSpecifiedInterfaceAddOperation(locale) : InterfaceDescription.getNamedInterfaceAddOperation(locale);
    }

    protected OperationResult installInterface(String name, ParsedInterfaceCriteria criteria, OperationContext context, ResultHandler resultHandler, ModelNode compensatingOp) {
        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOp);
    }

}
