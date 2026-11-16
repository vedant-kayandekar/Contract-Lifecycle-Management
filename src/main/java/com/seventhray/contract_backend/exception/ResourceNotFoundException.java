package com.seventhray.contract_backend.exception;

/** thrown whenever a request to databse resource ( contract user Document) cannot be found
 * cannot be found
 * 
 * give HTTP status 404 NOT FOUND
*/
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message){
        super(message);
    }
}
