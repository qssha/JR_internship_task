package com.space.controller;

import com.space.model.Ship;
import com.space.repository.ShipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(value = "/rest")
public class ShipController {
    private final ShipRepository repository;

    public ShipController(ShipRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/ships")
    public List<Ship> getShips(@RequestParam Map<String,String> allParams) {
        List<Ship> ships = dataFilter(allParams);

        if (allParams.get("order") != null) {
            ShipOrder order = ShipOrder.valueOf(allParams.get("order"));
            sortShips(ships, order);
        }

        int pageNumber = Integer.parseInt(allParams.getOrDefault("pageNumber", "0"));
        int pageSize = Integer.parseInt(allParams.getOrDefault("pageSize", "3"));
        return pageFilter(ships, pageNumber, pageSize);
    }

    @GetMapping("/ships/count")
    public Integer getCount(@RequestParam Map<String,String> allParams) {
        return dataFilter(allParams).size();
    }


    @PostMapping("/ships")
    public ResponseEntity<Ship> addShip(@RequestBody Ship newShip) {
        if (checkShip(newShip)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (newShip.getUsed() == null) newShip.setUsed(false);
        newShip.setRating(calculateRating(newShip));
        repository.save(newShip);
        return new ResponseEntity<>(newShip, HttpStatus.OK);
    }

    @GetMapping("/ships/{id:^(?!^0+$)\\d+$}")
    public ResponseEntity<Ship> getShipById(@PathVariable Long id) {
        if (!repository.findById(id).isPresent()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        else return new ResponseEntity<>(repository.findById(id).get(), HttpStatus.OK);
    }

    @PostMapping("/ships/{id:^(?!^0+$)\\d+$}")
    public ResponseEntity<Ship> updateShip(@RequestBody Ship newShip, @PathVariable Long id) {
        if (!repository.findById(id).isPresent()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        else {
            Ship ship = repository.findById(id).get();
            if (checkShipUpdate(newShip)) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            updateShip(ship, newShip);
            repository.save(ship);
            return new ResponseEntity<>(ship, HttpStatus.OK);
        }
    }

    @DeleteMapping("/ships/{id:^(?!^0+$)\\d+$}")
    public ResponseEntity<Ship> deleteShipById(@PathVariable Long id) {
        if (!repository.findById(id).isPresent()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        else {
            repository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    //BAD_REQUEST MAPPINGS
    @GetMapping("/ships/{id:^0+$|^(?!^\\d+$).+$}")
    public ResponseEntity<?> getShipById(@PathVariable String id) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/ships/{id:^0+$|^(?!^\\d+$).+$}")
    public ResponseEntity<?> updateShip(@RequestBody Ship newShip, @PathVariable String id) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping("/ships/{id:^0+$|^(?!^\\d+$).+$}")
    public ResponseEntity<?> deleteShipById(@PathVariable String id) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    private List<Ship> dataFilter(Map<String, String> request) {
        List<Ship> ships = repository.findAll();
        for (Map.Entry<String, String> entry :
                request.entrySet()) {
            switch (entry.getKey()) {
                case "name":
                    ships.removeIf(x -> !x.getName().contains(entry.getValue()));
                    break;
                case "planet":
                    ships.removeIf(x -> !x.getPlanet().contains(entry.getValue()));
                    break;
                case "shipType":
                    ships.removeIf(x -> !x.getShipType().toString().equals(entry.getValue()));
                    break;
                case "after":
                    ships.removeIf(x -> x.getProdDate().getTime() <= Long.parseLong(entry.getValue()));
                    break;
                case "before":
                    ships.removeIf(x -> x.getProdDate().getTime() >= Long.parseLong(entry.getValue()));
                    break;
                case "isUsed":
                    ships.removeIf(x -> !x.getUsed().equals(Boolean.parseBoolean(entry.getValue())));
                    break;
                case "minSpeed":
                    ships.removeIf(x -> x.getSpeed() <= Double.parseDouble(entry.getValue()));
                    break;
                case "maxSpeed":
                    ships.removeIf(x -> x.getSpeed() >= Double.parseDouble(entry.getValue()));
                    break;
                case "minCrewSize":
                    ships.removeIf(x -> x.getCrewSize() <= Integer.parseInt(entry.getValue()));
                    break;
                case "maxCrewSize":
                    ships.removeIf(x -> x.getCrewSize() >= Integer.parseInt(entry.getValue()));
                    break;
                case "minRating":
                    ships.removeIf(x -> x.getRating() <= Double.parseDouble(entry.getValue()));
                    break;
                case "maxRating":
                    ships.removeIf(x -> x.getRating() >= Double.parseDouble(entry.getValue()));
                    break;
            }
        }
        return ships;
    }

    private List<Ship> pageFilter(List<Ship> ships, int pageNumber, int pageSize) {
        int fromIndex = pageNumber * pageSize;
        int toIndex = fromIndex + pageSize;
        return ships.size() > toIndex ? ships.subList(fromIndex, toIndex) : ships.subList(fromIndex, ships.size());
    }

    private boolean checkShip(Ship ship) {
        return checkShipNullField(ship)
                || checkShipName(ship)
                || checkPlanetName(ship)
                || checkShipSpeed(ship)
                || checkShipCrewSize(ship)
                || checkShipProdDate(ship);
    }

    private boolean checkShipNullField(Ship ship) {
        return ship.getName() == null
                || ship.getPlanet() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null;
    }

    private boolean checkShipName(Ship ship) {
        return ship.getName().equals("")
                || ship.getName().length() > 50;
    }

    private boolean checkPlanetName(Ship ship) {
        return ship.getPlanet().equals("")
                || ship.getPlanet().length() > 50;
    }

    private boolean checkShipSpeed(Ship ship) {
        return ship.getSpeed() < 0.01
                || ship.getSpeed() > 0.99;
    }

    private boolean checkShipCrewSize(Ship ship) {
        return ship.getCrewSize() < 1
                || ship.getCrewSize() > 9999;
    }

    private boolean checkShipProdDate(Ship ship) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(ship.getProdDate());
        int prodYear = calendar.get(Calendar.YEAR);
        return ship.getProdDate().getTime() < 0 || prodYear < 2800 || prodYear > 3019;
    }

    private double calculateRating(Ship ship) {
        double k = ship.getUsed() ? 0.5 : 1.0;
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(ship.getProdDate());
        int prodYear = calendar.get(Calendar.YEAR);
        int currentYear = 3019;
        double rating = (80 * ship.getSpeed() * k) / (currentYear - prodYear + 1);
        return Double.parseDouble(String.format(Locale.US, "%.2f", rating));
    }

    private void sortShips(List<Ship> ships, ShipOrder order) {
        if (order == ShipOrder.ID) ships.sort(Comparator.comparing(Ship::getId));
        else if (order == ShipOrder.SPEED) ships.sort(Comparator.comparing(Ship::getSpeed));
        else if (order == ShipOrder.DATE) ships.sort(Comparator.comparing(Ship::getProdDate));
        else if (order == ShipOrder.RATING) ships.sort(Comparator.comparing(Ship::getRating));
    }

    private boolean checkShipUpdate(Ship newShip) {
        return newShip.getName() != null && checkShipName(newShip)
                || newShip.getPlanet() != null && checkPlanetName(newShip)
                || newShip.getProdDate() != null && checkShipProdDate(newShip)
                || newShip.getSpeed() != null && checkShipSpeed(newShip)
                || newShip.getCrewSize() != null && checkShipCrewSize(newShip);
    }

    private void updateShip(Ship ship, Ship newShip) {
        if (newShip.getName() != null) ship.setName(newShip.getName());
        if (newShip.getPlanet() != null) ship.setPlanet(newShip.getPlanet());
        if (newShip.getShipType() != null) ship.setShipType(newShip.getShipType());
        if (newShip.getProdDate() != null) ship.setProdDate(newShip.getProdDate());
        if (newShip.getUsed() != null) ship.setUsed(newShip.getUsed());
        if (newShip.getSpeed() != null) ship.setSpeed(newShip.getSpeed());
        if (newShip.getCrewSize() != null) ship.setCrewSize(newShip.getCrewSize());
        ship.setRating(calculateRating(ship));
    }
}
