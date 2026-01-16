# ldap2nextcloud

Create, update and delete Nextcloud user entries from a selected LDAP directory server.

## LDAP to NextCloud attribute mapping

The mapping will be done within a JavaScript function.
This `function` will be executed for every entry in the LDAP resultset.
 
```javascript
(
  function ldap2nextcloud( mode, user, ldapEntry, config )
  {
    // mode : "test", "create, "update"
    
    user.setFirstname(ldapEntry.getAttributeValue("givenname"));
    user.setLastname(ldapEntry.getAttributeValue("sn"));
    user.setEmail(ldapEntry.getAttributeValue("mail"));
    user.setPhone(ldapEntry.getAttributeValue("telephoneNumber"));
    user.setFax(ldapEntry.getAttributeValue("facsimileTelephoneNumber"));
    user.setWeb("https://www.myorg.de");
    user.setOrganization("MyOrg");
    user.setVerified(true);

    if ( "create" === mode || "test" === mode )
    {
      if ( ldapEntry.getAttributeValue("institute") === "CC" )
      {
        user.setDepartment( "CC" );
        user.setRoles( [ "Agent", "Customer" ] );
      }
      else
      {
        user.setRoles( [ "Customer" ] ); // array of String
      }
    }
  }
);
```

