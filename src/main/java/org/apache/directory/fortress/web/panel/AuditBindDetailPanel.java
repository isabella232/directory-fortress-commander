/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.fortress.web.panel;

import org.apache.directory.fortress.web.GlobalUtils;
import org.apache.directory.fortress.web.SelectModelEvent;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.directory.fortress.web.GlobalIds;
import org.apache.directory.fortress.core.*;
import org.apache.directory.fortress.core.rbac.Bind;
import org.apache.directory.fortress.core.rbac.User;
import org.apache.directory.fortress.core.util.attr.VUtil;

/**
 * @author Shawn McKinney
 * @version $Rev$
 * Date: 8/11/13
 */
public class AuditBindDetailPanel extends FormComponentPanel
{
    @SpringBean
    private AuditMgr auditMgr;
    @SpringBean
    private ReviewMgr reviewMgr;
    private static final Logger LOG = Logger.getLogger(AuditBindDetailPanel.class.getName());
    private Form detailForm;
    private Displayable display;
    private UserAuditDetailPanel userPanel;

    public Form getForm()
    {
        return this.detailForm;
    }

    public AuditBindDetailPanel( String id, Displayable display )
    {
        super(id);
        this.auditMgr.setAdmin( GlobalUtils.getRbacSession( this ) );
        this.reviewMgr.setAdmin( GlobalUtils.getRbacSession( this ) );
        this.detailForm = new AuditBindDetailForm( GlobalIds.DETAIL_FIELDS, new CompoundPropertyModel<Bind>(new Bind()));
        this.display = display;
        add( detailForm );
    }

    public class AuditBindDetailForm extends Form
    {
        private Component component;

        public AuditBindDetailForm(String id, final IModel<Bind> model)
        {
            super(id, model);
            add( new Label( GlobalIds.REQ_DN ) );
            add( new Label( GlobalIds.REQ_RESULT ) );
            add(  new Label( GlobalIds.REQ_START ) );
            userPanel = new UserAuditDetailPanel( GlobalIds.USERAUDITDETAILPANEL, new CompoundPropertyModel<User>(new User()) );
            add( userPanel );
            setOutputMarkupId( true );
        }

        @Override
        public void onEvent(final IEvent<?> event)
        {
            if (event.getPayload() instanceof SelectModelEvent )
            {
                SelectModelEvent modelEvent = (SelectModelEvent) event.getPayload();
                Bind bind = (Bind) modelEvent.getEntity();
                this.setModelObject(bind);
                String msg = "Bind: " + bind.getReqDN() + " has been selected";
                LOG.debug( ".onEvent SelectModelEvent: " + bind.getReqDN() );
                display.setMessage(msg);
                component = detailForm;

            }
            else if (event.getPayload() instanceof AjaxRequestTarget)
            {
                // only add the form to ajax target if something has changed...
                if (component != null)
                {
                    AjaxRequestTarget target = ((AjaxRequestTarget) event.getPayload());
                    LOG.debug( ".onEvent AjaxRequestTarget: " + target.toString() );
                    target.add(component);
                    component = null;
                }
                display.display((AjaxRequestTarget) event.getPayload());
            }
        }

        @Override
        protected void onBeforeRender()
        {
            if ( getModel() != null )
            {
                User user = null;
                // necessary to push the 'changed' model down into the aggregated panel:
                Bind bind = (Bind)detailForm.getModelObject();
                if( VUtil.isNotNullOrEmpty( bind.getReqDN() ))
                {
                    user = GlobalUtils.getUser(reviewMgr, bind.getReqDN());
                }
                if(user == null)
                {
                    user = new User();
                }
                IModel<User> userModel = new CompoundPropertyModel<User>(user);
                userPanel.setDefaultModel( userModel );
            }
            else
            {
                LOG.info( ".onBeforeRender null model object" );
            }
            super.onBeforeRender();
        }
    }
}