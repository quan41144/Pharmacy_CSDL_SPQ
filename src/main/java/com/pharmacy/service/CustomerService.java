package com.pharmacy.service;

import com.pharmacy.dto.CustomerDTO;
import com.pharmacy.entity.Customer;
import com.pharmacy.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CustomerDTO> liveSearchCustomers(String keyword) {
        List<Customer> list = customerRepository.liveSearchCustomers(keyword);
        return list.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        return toDTO(c);
    }

    @Transactional
    public CustomerDTO saveCustomer(CustomerDTO dto) {
        Customer customer;
        if (dto.getCustomerId() != null) {
            customer = customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng cần sửa"));
        } else {
            customer = new Customer();
        }

        customer.setFullName(dto.getFullName());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setGender(dto.getGender());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setNote(dto.getNote());

        Customer saved = customerRepository.save(customer);
        return toDTO(saved);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    private CustomerDTO toDTO(Customer c) {
        return CustomerDTO.builder()
                .customerId(c.getCustomerId())
                .fullName(c.getFullName())
                .dateOfBirth(c.getDateOfBirth())
                .gender(c.getGender())
                .phone(c.getPhone())
                .address(c.getAddress())
                .note(c.getNote())
                .build();
    }
}
