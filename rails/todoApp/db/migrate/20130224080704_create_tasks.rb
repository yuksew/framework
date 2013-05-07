class CreateTasks < ActiveRecord::Migration
  def change
    create_table :tasks do |t|
      t.boolean :isCompleted
      t.string :body

      t.timestamps
    end
  end
end
