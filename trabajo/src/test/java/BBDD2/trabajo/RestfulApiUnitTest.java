package BBDD2.trabajo;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class RestfulApiUnitTest {

	private String exampleSiteToken;
	
	
	@Before
	public void setUp(){
		post("/rest/utils"); //creates the test new site
		exampleSiteToken = get("/rest/sites/exampleSite").asString(); 
		put("/rest/utils/time/120000"); //sets token time limit to 2 minutes
	}
	
	@After
	public void tearDownTheWall(){
		//delete("/rest/utils");
	}
	
	@Test
	public void testAddAndGetSite(){
		post("/rest/sites/site1").then().assertThat().statusCode(201); //Create a new site
		post("/rest/sites/site1").then().assertThat().statusCode(412).and().assertThat().body(equalTo("Sitename already exists")); //Try to create the same site
	//	get("/rest/sites/site1").then().assertThat().statusCode(200).and().assertThat().body("token", ); //Get the site token (log-in)
		get("/rest/sites/site2").then().assertThat().statusCode(400).and().assertThat().body(equalTo("Sitename not found")); //Try to get a token for a non-existing site
	}

	@Test
	public void testAddGetAndDeleteCart(){
		post("/rest/sites/"+exampleSiteToken+"/carts/user1").then().assertThat().statusCode(201); //Create a new cart
		String cartToken = post("/rest/sites/"+exampleSiteToken+"/carts/user1").asString(); //Create another cart and get the token
		get("/rest/carts/"+cartToken).then().assertThat().body("userid", equalTo("user1")); //Get the cart and check the user
		delete("/rest/carts/"+cartToken).then().assertThat().statusCode(204); //Delete the cart
		delete("/rest/carts/"+cartToken).then().assertThat().statusCode(400).and().assertThat().body(equalTo("Cart not found")); //Try to delete a non-existing cart
		get("/rest/carts/"+cartToken).then().assertThat().statusCode(400).and().assertThat().body(equalTo("Cart not found")); //Try to get a token for a non-existing cart
	}
	
	@Test
	public void testAddGetAndDeleteProducts() throws InterruptedException{
		String cartToken = post("/rest/sites/"+exampleSiteToken+"/carts/user1").asString(); //Create another cart and get the token
		long time = System.currentTimeMillis();
		post("/rest/carts/"+cartToken+"/products/product1?price=10").then().assertThat().statusCode(201); //Create a product in the cart
		post("/rest/carts/"+cartToken+"/products/product2").then().assertThat().statusCode(400).and().body(equalTo("The price of the product is needed")); //Try to create a product without the price
		get("/rest/carts/"+cartToken+"/products/product1").then().assertThat().body("quantity", equalTo("1")); //Get the product of the cart
		System.out.println(put("/rest/carts/"+cartToken+"/products/product1?price=20&quantity=4").asString()); //Change the price and adds 4 quantity of the product
		get("/rest/carts/"+cartToken).then().assertThat().body("products[0].price", equalTo("20.0"), "products[0].quantity", equalTo("5"));
		put("/rest/carts/"+cartToken+"/products/product1"); //Add 1 to the product quantity
		get("/rest/carts/"+cartToken+"/products/product1").then().assertThat().body("quantity", equalTo("6")); //Get the product of the cart
		get("/rest/carts/"+cartToken+"/products/product2").then().assertThat().statusCode(400).and().assertThat().body(equalTo("Product not found")); //Try to get a non-existing product
		post("/rest/carts/"+cartToken+"/products/product2?price=20"); //Add a product to the cart
		get("/rest/carts/"+cartToken).then().assertThat().body("products", hasSize(2), "total", equalTo("40.0")); //Checks the values of the cart
		delete("/rest/carts/"+cartToken+"/products/product1"); //Delete the product1 of the cart
		get("/rest/carts/"+cartToken+"/products/product1").then().assertThat().statusCode(400).and().assertThat().body(equalTo("Product not found")); //Try to get a non-existing product	
		get("/rest/carts/"+cartToken).then().assertThat().body("products", hasSize(1));
		get("/rest/carts/"+cartToken+"/products/product2").then().assertThat().body("productid", equalTo("product2"));
		Thread.sleep(120000-(System.currentTimeMillis() - time));
		get("/rest/carts/"+cartToken).then().assertThat().body(equalTo("Cart time limit exceed")).and().statusCode(403); //Try to continue using the cart after 2 minutes
	}
}
