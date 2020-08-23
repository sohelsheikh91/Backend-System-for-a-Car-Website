package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.Address;
import com.udacity.vehicles.client.maps.MapsClient;
import com.udacity.vehicles.client.prices.Price;
import com.udacity.vehicles.client.prices.PriceClient;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */

@Service
public class CarService {

    @Autowired
    @Qualifier("pricing")
    WebClient webClientPricing;

    @Autowired
    @Qualifier("maps")
    WebClient webClientMaps;

    private final CarRepository repository;

    public CarService(CarRepository repository) {
        /**
         * TODO: Add the Maps and Pricing Web Clients you create
         *   in `VehiclesApiApplication` as arguments and set them here.
         */
        this.repository = repository;
    }

    /**
     * Gathers a list of all vehicles
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        return repository.findAll();
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {
        /**
         *   If it does not exist, throw a CarNotFoundException
         *   Remove the below code as part of your implementation.
         */
        Optional<Optional<Car>> optionalCar = Optional.ofNullable(repository.findById(id));
        Optional<Car> car = optionalCar.orElseThrow(CarNotFoundException::new);
        /**
         *   to get the price based on the `id` input'
         * Note: The car class file uses @transient, meaning you will need to call
         *   the pricing service each time to get the price.
         */
        PriceClient priceClient = new PriceClient(webClientPricing);
        String price = priceClient.getPrice(id);


//        Price price = webClientPricing.get().uri(uriBuilder -> uriBuilder
//                .path("/services/price/")
//                .queryParam("vehicleId", id).build())
//                .retrieve()
//                .bodyToMono(Price.class)
//                .block();

        car.get().setPrice(price);

        /**
         *   to get the address for the vehicle. You should access the location
         *   from the car object and feed it to the Maps service.
         * Note: The Location class file also uses @transient for the address,
         * meaning the Maps service needs to be called each time for the address.
         */
//        Address address = webClientMaps.get().uri(uriBuilder -> uriBuilder
//                .path("/maps/")
//                .queryParam("lat", car.get().getLocation().getLat())
//                .queryParam("lon", car.get().getLocation().getLon())
//                .build())
//                .retrieve()
//                .bodyToMono(Address.class)
//                .block();
//        mapper.map(Objects.requireNonNull(address), car.get().getLocation());
        ModelMapper mapper = new ModelMapper();
        mapper.addMappings(new PropertyMap<Address, Location>() {

            @Override
            protected void configure() {
                map().setAddress(source.getAddress());
                map().setCity(source.getCity());
                map().setState(source.getState());
                map().setZip(source.getZip());
            }
        });
        MapsClient mapsClient = new MapsClient(webClientMaps,mapper);
        car.get().setLocation( mapsClient.getAddress(car.get().getLocation()));

        return car.get();
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {
        if (car.getId() != null) {
            return repository.findById(car.getId())
                    .map(carToBeUpdated -> {
                        carToBeUpdated.setDetails(car.getDetails());
                        carToBeUpdated.setLocation(car.getLocation());
                        return repository.save(carToBeUpdated);
                    }).orElseThrow(CarNotFoundException::new);
        }

        return repository.save(car);
    }

    /**
     * Deletes a given car by ID
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {
        /**
         *   If it does not exist, throw a CarNotFoundException
         */
        if(repository.findById(id).isPresent()){
            repository.deleteById(id);
        }

        /**
         */


    }
}
