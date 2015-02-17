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
package org.apache.directory.fortress.web;

import org.apache.log4j.Logger;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.directory.fortress.web.panel.UserListPanel;
import org.apache.directory.fortress.core.DelReviewMgr;
import org.apache.directory.fortress.core.ReviewMgr;
import org.apache.directory.fortress.core.rbac.AdminRole;
import org.apache.directory.fortress.core.rbac.OrgUnit;
import org.apache.directory.fortress.core.rbac.Permission;
import org.apache.directory.fortress.core.rbac.Role;
import org.apache.directory.fortress.core.rbac.Session;
import org.apache.directory.fortress.core.rbac.User;
import org.apache.directory.fortress.core.util.attr.VUtil;
import org.apache.directory.fortress.core.SecurityException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UserListModel extends Model<SerializableList<User>>
{
    /** Default serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    @SpringBean
    private ReviewMgr reviewMgr;
    @SpringBean
    private DelReviewMgr delReviewMgr;
    private static final Logger log = Logger.getLogger(UserListModel.class.getName());
    private transient User user;
    private transient Permission perm;
    private transient SerializableList<User> users = null;

    /**
     * Default constructor
     */
    public UserListModel( Session session )
    {
        init( session );
    }

    
    /**
     * User contains the search arguments.
     *
     * @param user
     */
    public UserListModel( User user, Session session )
    {
        this.user = user;
        init( session );
        log.debug( "constructor userId: " + user != null ? user.getUserId() : "null" );
    }

    
    public UserListModel( Permission perm, Session session )
    {
        this.perm = perm;
        init( session );
        log.debug( "constructor perm: " + perm != null ? perm.getObjName() : "null" );
    }
    

    private void init( Session session )
    {
        Injector.get().inject( this );
        reviewMgr.setAdmin( session );
    }
    

    /**
     * This data is bound for {@link UserListPanel}
     *
     * @return T extends List<User> users data will be bound to panel data view component.
     */
    @Override
    public SerializableList<User> getObject()
    {
        if (users != null)
        {
            log.debug( ".getObject count: " + user != null ? users.size() : "null" );
            return users;
        }
        
        if ( ( user == null ) && ( perm == null  ))
        {
            log.debug( ".getObject null" );
            users = new SerializableList<User>( new ArrayList<User>() );
        }
        else
        {
            //log.debug(".getObject userId: " + user != null ? user.getUserId() : "null");
            users = new SerializableList<User>( getList( user ) );
        }
        
        return users;
    }

    
    @Override
    public void setObject( SerializableList<User> object )
    {
        log.debug(".setObject count: " + object != null ? object.size() : "null");
        users = object;
    }
    

    @Override
    public void detach()
    {
        //log.debug(".detach");
        users = null;
        user = null;
    }
    

    public List<User> getList(User user)
    {
        List<User> usersList = null;
        
        try
        {
            if ( perm != null )
            {
                Set<String> users = reviewMgr.authorizedPermissionUsers( perm );
                
                if ( VUtil.isNotNullOrEmpty( users ) )
                {
                    usersList = new ArrayList<User>();
                    
                    for(String userId : users)
                    {
                        User user1 = reviewMgr.readUser( new User( userId ) );
                        usersList.add( user1 );
                    }
                }
            }
            else if( VUtil.isNotNullOrEmpty( user.getOu() ) )
            {
                usersList = reviewMgr.findUsers( new OrgUnit( user.getOu(), OrgUnit.Type.USER ) );
            }
            else if ( VUtil.isNotNullOrEmpty( user.getRoles() ) )
            {
                usersList = reviewMgr.assignedUsers( new Role( user.getRoles().get( 0 ).getName() ) );
            }
            else if ( VUtil.isNotNullOrEmpty( user.getAdminRoles() ) )
            {
                usersList = delReviewMgr.assignedUsers( new AdminRole( user.getAdminRoles().get( 0 ).getName() ) );
            }
            else
            {
                usersList = reviewMgr.findUsers( user );
            }
            // sort list by userId:
            if( VUtil.isNotNullOrEmpty( usersList ))
            {
                Collections.sort( ( List<User> ) usersList, new Comparator<User>()
                {
                    @Override
                    public int compare(User u1, User u2)
                    {
                        return u1.getUserId().compareToIgnoreCase( u2.getUserId() );
                    }
                } );
            }
        }
        catch ( SecurityException se )
        {
            String error = ".getList caught SecurityException=" + se;
            log.warn( error );
        }
        
        return usersList;
    }
}
