# React SPA's and Direct vs BFF pattern

In the field of engineering React UI's and Springboot being back-ends, I am hearing discussions around how to integrate Springboot API's on React UI's. React developers usually are tempting to boot there SPA Single Page UI React app as static content from NGINX web server, read [How To Serve React Application With NGINX and Docker](https://medium.com/bb-tutorials-and-thoughts/how-to-serve-react-application-with-nginx-and-docker-9c51ac2c50ba).

React developers are eager to access the underlying API's directly from there React code, this we know to be the MSA Direct pattern. Direct accessing your API's  from your ReactJS UI might be tempting and easy to set up and quite sufficient for relatively small apps, but causes quite a few challenges that become more and more apparent and troublesome as your application grows in size and complexity.

Read about different MSA patterns like [Direct, BFF and Gateway](https://tsh.io/blog/design-patterns-in-microservices-api-gateway-bff-and-more/) and how that relates to concerns like performance, scalability, complexity and security.

![SPA](ApiGateway.png)

Once your infrastructure is not supporting central gateway solutions like kong or an integrated ISTIO kinf of solution on kubernetes you might want to go for the BFF Backend For Frontend pattern.

We have the following BFF scenarios:

1. Providing a layer of logic and data abstracting (more like the traditional web app) 
2. Blindly forwarding HTTP requests, this might be a tedious thing to do if you start programming something to accomplish this yourself.
3. A mix of both, this is probably most frequently scenario we can think of.

No we have that challenge at 2 to keep DRY and maintainable. 

My suggestion and probably the most standard, scaleable and portable solution is [Spring Cloud Gateway routing handlers](https://www.baeldung.com/spring-cloud-gateway-routing-predicate-factories#anatomy_of_a_predicate), more specifically 
the [Path Route Predicate Factory](2. https://www.baeldung.com/spring-cloud-gateway#6-path-route-predicate-factory) that allows you to programmatically (Fluent API) or declaratively define your path's to your downstream AP's using properties or YAML file.


Take a look at [this example](https://github.com/spring-cloud-samples/spring-cloud-gateway-sample/blob/master/src/main/java/com/example/demogateway/DemogatewayApplication.java), this allows you to have a mix of different kind route predicates based on different conditions to reaching for down-stream API's, allowing you to add additionally embedded REST endpoints, clearly seperated.
 
I suggest to defining a standard naming pattern for downstream path's to start with /api, that will exclude all embedded logic.



