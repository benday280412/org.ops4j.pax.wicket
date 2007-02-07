/*  Copyright 2007 Niclas Hedhman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.wicket.api;

import wicket.authorization.strategies.role.Roles;


/** PaxWicketAuthentication represents a subset of the current Session.
 *
 * Typical usage would be;
 * <code><pre>
 * import wicket.authentication.AuthenticatedWebSession;
 * import wicket.authorization.AuthorizationException;
 * import wicket.authorization.strategies.role.Roles;
 *
 * :
 *
 * PaxWicketAuthentication auth = (PaxWicketAuthentication) AuthenticatedWebSession.get();
 * Roles roles = auth.getRoles();
 * if( ! roles.hasRole( MyRoles.MEMBER ) )
 * {
 *     throw new AuthorizationException( "Only members are allowed in this area." );
 * }
 * String username = auth.getLoggedInUser();
 * Label userLabel = new Label( "username", username );
 * add( userLabel ); 
 * </pre></code>
 */
public interface PaxWicketAuthentication
{

    /**
     *
     * @return The user that is logged in, in the request that this PaxWicketAuthentication is part of.
     */
    String getLoggedInUser();

    /**
     *
     * @return The Roles of the user that is logged in.
     */
    Roles getRoles();
}