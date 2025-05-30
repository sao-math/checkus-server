package saomath.checkusserver.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBase<T> {
    private boolean success;
    private String message;
    private T data;
    
    public static <T> ResponseBase<T> success(T data) {
        return new ResponseBase<>(true, "성공", data);
    }
    
    public static <T> ResponseBase<T> success(String message, T data) {
        return new ResponseBase<>(true, message, data);
    }
    
    public static <T> ResponseBase<T> error(String message) {
        return new ResponseBase<>(false, message, null);
    }
    
    public static <T> ResponseBase<T> error(String message, T data) {
        return new ResponseBase<>(false, message, data);
    }
}
