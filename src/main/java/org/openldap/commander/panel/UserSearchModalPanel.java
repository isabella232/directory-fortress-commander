/*
 * This work is part of OpenLDAP Software <http://www.openldap.org/>.
 *
 * Copyright 1998-2014 The OpenLDAP Foundation.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted only as authorized by the OpenLDAP
 * Public License.
 *
 * A copy of this license is available in the file LICENSE in the
 * top-level directory of the distribution or, alternatively, at
 * <http://www.OpenLDAP.org/license.html>.
 */
package org.openldap.commander.panel;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.openldap.commander.GlobalIds;
import org.openldap.commander.GlobalUtils;
import org.openldap.fortress.ReviewMgr;
import org.openldap.fortress.rbac.User;

/**
 *
 * @author Shawn McKinney
 * @version $Rev$
 */
public class UserSearchModalPanel extends Panel
{
    @SpringBean
    private ReviewMgr reviewMgr;
    private static final Logger LOG = Logger.getLogger(UserSearchModalPanel.class.getName());
    private ModalWindow window;
    private User userSelection;
    private String userSearchVal;

    /**
     * @param id
     */
    public UserSearchModalPanel( String id, ModalWindow window)
    {
        super( id );
        // TODO: add later:
        this.reviewMgr.setAdmin( GlobalUtils.getRbacSession( this ) );
        this.window = window;
        loadPanel();
    }

    public void loadPanel()
    {
        LoadableDetachableModel requests = getListViewModel();
        PageableListView policyView = createListView( requests );
        add( policyView );
        add( new AjaxPagingNavigator( "usernavigator", policyView ) );
    }

    private PageableListView createListView( final LoadableDetachableModel requests )
    {
        final PageableListView listView = new PageableListView( "userdataview", requests, 10 )
        {
            @Override
            protected void populateItem( final ListItem item )
            {
                final User modelObject = ( User ) item.getModelObject();
                item.add( new AjaxLink<Void>( GlobalIds.SELECT )
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target)
                    {
                        userSelection = modelObject;
                        window.close( target );
                    }
                } );
                item.add( new Label( GlobalIds.USER_ID, new PropertyModel( item.getModel(), GlobalIds.USER_ID ) ) );
                item.add( new Label( GlobalIds.DESCRIPTION, new PropertyModel( item.getModel(), GlobalIds.DESCRIPTION ) ) );
                item.add( new Label( GlobalIds.NAME, new PropertyModel( item.getModel(), GlobalIds.NAME ) ) );
                item.add( new Label( GlobalIds.OU, new PropertyModel( item.getModel(), GlobalIds.OU ) ) );
                item.add( new Label( GlobalIds.TITLE, new PropertyModel( item.getModel(), GlobalIds.TITLE ) ) );
                item.add( new JpegImage( GlobalIds.JPEGPHOTO )
                {
                    @Override
                    protected byte[] getPhoto()
                    {
                        byte[] photo;
                        photo = modelObject.getJpegPhoto();
                        return photo;
                    }
                } );
            }
        };
        return listView;
    }

    private LoadableDetachableModel getListViewModel()
    {
        final LoadableDetachableModel ret = new LoadableDetachableModel()
        {
            @Override
            protected Object load()
            {
                List<User> users = null;
                try
                {
                    userSelection = null;
                    if(userSearchVal == null)
                        userSearchVal = "";
                    users = reviewMgr.findUsers( new User( userSearchVal ) );
                }
                catch ( org.openldap.fortress.SecurityException se )
                {
                    String error = "loadPanel caught SecurityException=" + se;
                    LOG.error( error );
                }
                return users;
            }
        };
        return ret;
    }

    public User getUserSelection()
    {
        return userSelection;
    }

    public void setSearchVal( String searchVal )
    {
        this.userSearchVal = searchVal;
    }
}