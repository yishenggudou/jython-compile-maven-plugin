
Feature: Fibonacci
  In order to show off my programming skills
  As a math nerd
  I want to calculate the fibonacci series
  
  Scenario: 1
    When I calculate the first 1 fibonacci numbers
    Then it should give me [1]

  Scenario: 2
    When I calculate the first 2 fibonacci numbers
    Then it should give me [1, 1]
  
  Scenario Outline: Series up to 10
    When I calculate the first <n> fibonacci numbers
    Then it should give me <series>
    
    Examples:
      | n   | series                                 |
      | 3   | [1, 1, 2]                              |
      | 4   | [1, 1, 2, 3]                           |
      | 5   | [1, 1, 2, 3, 5]                        |
      | 6   | [1, 1, 2, 3, 5, 8]                     |
      | 11  | [1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89] |
  