package com.transit.graphbased_v2.controller;

import com.transit.graphbased_v2.controller.dto.ObjectDTO;
import com.transit.graphbased_v2.exceptions.BadRequestException;
import com.transit.graphbased_v2.exceptions.ValidationException;
import com.transit.graphbased_v2.service.ObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/object")
public class ObjectController {

    @Autowired
    private Validator validator;

    @Autowired
    private ObjectService objectService;


    @PostMapping
    public ResponseEntity<Object> createObjectNode(@RequestBody ObjectDTO requestDTO) throws BadRequestException {


        return new ResponseEntity<>(objectService.createObject(requestDTO), HttpStatus.CREATED);

    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateObjectNode(@PathVariable("id") UUID id, @RequestBody ObjectDTO requestDTO) throws BadRequestException {
        requestDTO.setObjectId(id);
        Set<ConstraintViolation<ObjectDTO>> violations = validator.validate(requestDTO);
        if (!violations.isEmpty()) {
            Set<String> failures = violations.stream().map(contraints -> contraints.getRootBeanClass().getSimpleName() + "." + contraints.getPropertyPath() + " " + contraints.getMessage()).collect(Collectors.toSet());
            String failure = "";
            for (String fail : failures) {
                failure = failure + fail + ",";
            }

            throw new ValidationException(failure);
        }
        return new ResponseEntity<>(objectService.updateObject(requestDTO), HttpStatus.OK);

    }


    @GetMapping("/{id}")
    public ResponseEntity getObjectNode(@PathVariable("id") UUID id) {


        var opt = objectService.getObject(id);

        if (opt.isEmpty()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity(opt.get(), HttpStatus.OK);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity deleteObjectNode(@PathVariable("id") UUID id, @RequestParam(value = "requestedById") UUID requestedById) {

        if (requestedById == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        objectService.deleteObject(id, requestedById);
        return new ResponseEntity(HttpStatus.OK);

    }

}
