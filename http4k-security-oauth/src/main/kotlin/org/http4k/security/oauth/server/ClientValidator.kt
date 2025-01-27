package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Uri

/**
 * Provides a consistent way to retrieve clients attempting to use an authorization code flow
 */
interface ClientValidator {
    /**
     * - check that client_id is a valid, registered app
     */
    fun validateClientId(request: Request, clientId: ClientId): Boolean

    /**
     * - redirection URI is one of the allowed ones for that client
     */
    fun validateRedirection(request: Request, clientId: ClientId, redirectionUri: Uri): Boolean

    /**
     * Validate that credentials provided by the client match its registration records
     */
    fun validateCredentials(request: Request, clientId: ClientId, clientSecret: String): Boolean
}