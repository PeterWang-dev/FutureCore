import "DPI-C" context function void ebreak(input int status);

module ebreak_dpi (
    input wire clk_i,
    input wire rst_ni,
    input wire s_valid_i,
    input wire [31:0] s_status_i
);

  always_comb begin : ebreak_call
    if (rst_ni & s_valid_i) begin
      ebreak(s_status_i);
    end
  end
endmodule

