package com.ws.chat.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Setting implements Serializable {

     private static final long serialVersionUID = 1L;

     private boolean notification;
}
