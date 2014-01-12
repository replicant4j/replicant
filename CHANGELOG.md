## v0.4.14:
* Add template method AbstractDataLoaderService.shouldValidateOnLoad() that will
  validate the entity repository on each change. Useful to override and return true
  during development or in debug mode.

## v0.4.8:
* Change AbstractReplicationInterceptor so that subclasses must override a template
  method to provide the EntityManager. The purpose of this change is to allow for
  the use of this library in applications that have multiple persistence contexts.
